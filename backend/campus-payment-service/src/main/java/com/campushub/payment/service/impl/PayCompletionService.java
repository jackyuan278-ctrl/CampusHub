package com.campushub.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.common.utils.BizNoGenerator;
import com.campushub.payment.domain.PayStatus;
import com.campushub.payment.domain.po.PayEventOutbox;
import com.campushub.payment.domain.po.PayOrder;
import com.campushub.payment.mapper.PayEventOutboxMapper;
import com.campushub.payment.mapper.PayOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/** Atomically records the paid state and the event that Trade must receive. */
@Service
@RequiredArgsConstructor
public class PayCompletionService {
    private final PayOrderMapper payOrderMapper;
    private final PayEventOutboxMapper outboxMapper;
    private final PayEventPublisher payEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaidAndCreateEvent(Long payOrderId, Long orderId) {
        PayOrder payOrder = payOrderMapper.selectById(payOrderId);
        if (payOrder == null) {
            return false;
        }
        // tb_pay_event_outbox.payment_no 非空且与 event_type 组成唯一键：这里必须把支付业务号带进去。
        // 历史数据可能没有 payment_no（早期版本未生成），补一个并随状态更新一起落库，避免空值插入失败。
        String paymentNo = payOrder.getPaymentNo();
        if (paymentNo == null || paymentNo.isBlank()) {
            paymentNo = BizNoGenerator.payment();
        }
        int updated = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getId, payOrderId)
                .eq(PayOrder::getStatus, PayStatus.PROCESSING)
                .set(PayOrder::getStatus, PayStatus.PAID)
                .set(PayOrder::getPaymentNo, paymentNo)
                .set(PayOrder::getPayTime, LocalDateTime.now()));
        if (updated == 0) return false;
        PayEventOutbox event = new PayEventOutbox();
        event.setPaymentNo(paymentNo);
        event.setBizOrderNo(orderId);
        event.setPublished(0);
        event.setCreateTime(LocalDateTime.now());
        outboxMapper.insert(event);
        // 提交后立刻推一次，把最长 3 秒的轮询空窗压到毫秒级；推送失败时记录仍是 published=0，定时任务兜底。
        // 必须挂在 afterCommit：事务没提交就发，一旦回滚就成了指向「不存在的支付」的幽灵通知。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                payEventPublisher.publishOne(event);
            }
        });
        return true;
    }
}
