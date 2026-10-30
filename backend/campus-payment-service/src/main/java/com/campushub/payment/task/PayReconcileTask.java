package com.campushub.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.api.client.TradeClient;
import com.campushub.api.client.UserClient;
import com.campushub.api.dto.OrderDTO;
import com.campushub.common.Result;
import com.campushub.payment.domain.PayStatus;
import com.campushub.payment.domain.po.PayOrder;
import com.campushub.payment.mapper.PayOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关单与支付竞态的对账补偿。
 *
 * <p>场景：订单 30 分钟超时被关（status=5），但用户在这前后刚好付了款。
 * 关单的条件更新（where status=1）会赢，支付成功消息被 Trade 静默跳过，
 * 结果就是「钱扣了、订单已关闭、没人管」。这里按分钟扫一遍：
 * 支付单 PAID + 订单已 CLOSED → 退回余额。
 *
 * <p>幂等：退款前先做 PAID → REFUNDED 的条件更新，这一行只会成功一次；
 * 即使服务重启或多实例并发，第二个执行者拿不到退款权，不会重复退款。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayReconcileTask {

    /** tb_order.status：已关闭（与 OrderServiceImpl 保持一致的取值） */
    private static final int ORDER_STATUS_CLOSED = 5;
    /** 刚支付完的订单可能还在 MQ 途中，先放过 2 分钟再看 */
    private static final int GRACE_MINUTES = 2;
    private static final int BATCH_LIMIT = 50;

    private final PayOrderMapper payOrderMapper;
    private final TradeClient tradeClient;
    private final UserClient userClient;

    @Scheduled(fixedDelay = 60_000)
    public void refundClosedOrders() {
        List<PayOrder> paidOrders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getStatus, PayStatus.PAID)
                .le(PayOrder::getPayTime, LocalDateTime.now().minusMinutes(GRACE_MINUTES))
                .orderByAsc(PayOrder::getId)
                .last("LIMIT " + BATCH_LIMIT));
        for (PayOrder payOrder : paidOrders) {
            refundIfOrderClosed(payOrder);
        }
    }

    private void refundIfOrderClosed(PayOrder payOrder) {
        try {
            Result<OrderDTO> result = tradeClient.queryOrderById(payOrder.getBizOrderNo());
            OrderDTO order = result == null ? null : result.getData();
            if (order == null) {
                // 查不到就下轮再看，绝不凭猜测退钱
                return;
            }
            if (!Integer.valueOf(ORDER_STATUS_CLOSED).equals(order.getStatus())) {
                return;
            }
            // 抢占退款权：这一步成功才代表「本次由我负责退款」
            boolean claimed = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                    .eq(PayOrder::getId, payOrder.getId())
                    .eq(PayOrder::getStatus, PayStatus.PAID)
                    .set(PayOrder::getStatus, PayStatus.REFUNDED)) > 0;
            if (!claimed) {
                return;
            }
            Result<Void> refund = userClient.refundBalance(
                    payOrder.getPayUserId(), payOrder.getId(), payOrder.getAmount());
            if (refund == null || !Integer.valueOf(200).equals(refund.getCode())) {
                log.error("订单{}已关闭但退款失败，支付单{}需人工介入",
                        payOrder.getBizOrderNo(), payOrder.getId());
                return;
            }
            log.info("订单{}已关闭，已退款 {} 分，支付单{}",
                    payOrder.getBizOrderNo(), payOrder.getAmount(), payOrder.getId());
        } catch (Exception e) {
            log.warn("支付单{}对账失败，下轮重试", payOrder.getId(), e);
        }
    }
}
