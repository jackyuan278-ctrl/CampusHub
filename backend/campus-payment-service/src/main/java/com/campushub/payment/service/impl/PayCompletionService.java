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

import java.time.LocalDateTime;

/** Atomically records the paid state and the event that Trade must receive. */
@Service
@RequiredArgsConstructor
public class PayCompletionService {
    private final PayOrderMapper payOrderMapper;
    private final PayEventOutboxMapper outboxMapper;

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
        return true;
    }
}
