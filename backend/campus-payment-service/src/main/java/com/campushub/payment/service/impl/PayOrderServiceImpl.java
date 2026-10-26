package com.campushub.paymentment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.api.client.TradeClient;
import com.campushub.api.client.UserClient;
import com.campushub.api.dto.OrderDTO;
import com.campushub.common.BizException;
import com.campushub.common.Result;
import com.campushub.common.interceptor.UserContext;
import com.campushub.paymentment.domain.PayStatus;
import com.campushub.paymentment.domain.dto.PayFormDTO;
import com.campushub.paymentment.domain.po.PayOrder;
import com.campushub.paymentment.mapper.PayOrderMapper;
import com.campushub.paymentment.service.IPayOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements IPayOrderService {
    private static final String PAY_LOCK_PREFIX = "campushub:pay:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 30;
    private static final long PROCESSING_TIMEOUT_MINUTES = 5;

    private final TradeClient tradeClient;
    private final UserClient userClient;
    private final PayCompletionService payCompletionService;
    private final StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = PayLockScripts.UNLOCK;

    @Override
    public void payOrder(Long orderId, PayFormDTO payFormDTO) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }

        // 锁只用于削峰；真正的并发正确性由支付单状态的数据库条件更新保证。
        String lockKey = PAY_LOCK_PREFIX + orderId;
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(409, "支付处理中，请稍后重试");
        }

        try {
            payWithLock(orderId, userId, payFormDTO);
        } finally {
            // Lua 比较并删除，避免锁过期后误删其他请求新拿到的锁。
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockToken);
        }
    }

    private void payWithLock(Long orderId, Long userId, PayFormDTO payFormDTO) {
        // 查询 Trade 订单时由 trade 服务按登录态校验归属，不能只信任前端传入的 orderId。
        // 注意：这里处理的是 Pay 支付单，Trade 订单在扣款成功通知到达前仍保持待支付。
        Result<OrderDTO> orderResult = tradeClient.queryOrderById(orderId);
        if (orderResult == null || orderResult.getData() == null) {
            throw new BizException(400, "订单不存在或无权支付");
        }
        OrderDTO order = orderResult.getData();
        if (order.getTotalFee() == null || order.getTotalFee() <= 0) {
            throw new BizException(400, "订单金额无效");
        }

        PayOrder payOrder = lambdaQuery().eq(PayOrder::getBizOrderNo, orderId).one();
        if (payOrder == null) {
            // 首次支付时创建支付单；唯一索引处理多实例下的并发创建。
            PayOrder created = new PayOrder();
            created.setBizOrderNo(orderId);
            created.setPayUserId(userId);
            created.setAmount(order.getTotalFee());
            created.setStatus(PayStatus.UNPAID);
            created.setCreateTime(LocalDateTime.now());
            try {
                save(created);
                payOrder = created;
            } catch (DuplicateKeyException concurrentCreate) {
                payOrder = lambdaQuery().eq(PayOrder::getBizOrderNo, orderId).one();
                if (payOrder == null) {
                    throw concurrentCreate;
                }
            }
        }

        if (!userId.equals(payOrder.getPayUserId()) || !order.getTotalFee().equals(payOrder.getAmount())) {
            throw new BizException(400, "支付单用户或金额与订单不一致");
        }
        if (Objects.equals(payOrder.getStatus(), PayStatus.PAID)) {
            return; // 已支付按成功响应，保证接口幂等。
        }
        if (!Objects.equals(order.getStatus(), PayStatus.UNPAID)) {
            throw new BizException(400, "订单当前状态不可支付");
        }

        // 数据库 CAS 才是最终闸门：只允许 UNPAID -> PROCESSING 的一个请求进入扣款。
        boolean claimed = lambdaUpdate()
                .eq(PayOrder::getId, payOrder.getId())
                .eq(PayOrder::getStatus, PayStatus.UNPAID)
                .set(PayOrder::getStatus, PayStatus.PROCESSING)
                .update();

        if (!claimed) {
            PayOrder latest = getById(payOrder.getId());
            if (latest != null && Objects.equals(latest.getStatus(), PayStatus.PAID)) {
                return;
            }
            // 处理中超时后才尝试恢复认领；乐观条件包含旧更新时间，保证只有一个恢复者成功。
            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
            if (latest != null && Objects.equals(latest.getStatus(), PayStatus.PROCESSING)
                    && latest.getUpdateTime() != null && latest.getUpdateTime().isBefore(staleBefore)) {
                claimed = lambdaUpdate()
                        .eq(PayOrder::getId, latest.getId())
                        .eq(PayOrder::getStatus, PayStatus.PROCESSING)
                        .eq(PayOrder::getUpdateTime, latest.getUpdateTime())
                        .set(PayOrder::getUpdateTime, LocalDateTime.now())
                        .update();
            }
            if (!claimed) {
                throw new BizException(409, "支付处理中，请稍后重试");
            }
        }

        String payType = payFormDTO.getPayType();
        if (!Objects.equals(payType, "balance") && !Objects.equals(payType, "mock")) {
            // 在触发外部扣款前即可确定的错误，安全恢复为待支付。
            resetToUnpaid(payOrder.getId());
            throw new BizException(400, "未知付款方式");
        }

        if (Objects.equals(payType, "balance")) {
            // orderId 是用户服务本地事务的唯一幂等键；超时后重试不会重复扣余额。
            // Feign 抛异常/超时意味着结果未知，保留 PROCESSING，不能误判为失败后直接重扣。
            Result<Void> debitResult;
            try {
                debitResult = userClient.deductBalance(userId, orderId, order.getTotalFee());
            } catch (RuntimeException uncertain) {
                log.warn("扣款结果未知，保留处理中状态，orderId={}", orderId, uncertain);
                throw new BizException(409, "扣款结果待核对，请稍后重试");
            }
            if (debitResult == null || !Objects.equals(debitResult.getCode(), 200)) {
                // 收到明确业务失败响应，用户服务事务已回滚，方可释放处理中状态。
                resetToUnpaid(payOrder.getId());
                throw new BizException(400, debitResult == null ? "扣款失败" : debitResult.getMsg());
            }
        }

        // 在本地事务中原子地标记已支付并写入 Outbox；支付事件由后台任务可靠重试。
        if (!payCompletionService.markPaidAndCreateEvent(payOrder.getId(), orderId)) {
            throw new BizException(409, "支付结果待核对，请勿重复支付");
        }
    }

    private void resetToUnpaid(Long payOrderId) {
        lambdaUpdate().eq(PayOrder::getId, payOrderId)
                .eq(PayOrder::getStatus, PayStatus.PROCESSING)
                .set(PayOrder::getStatus, PayStatus.UNPAID)
                .update();
    }
}




