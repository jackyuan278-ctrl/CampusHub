package com.campushub.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.api.constants.MqConstants;
import com.campushub.payment.domain.po.PayEventOutbox;
import com.campushub.payment.mapper.PayEventOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayEventPublisher {
    private final PayEventOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 3000)
    public void publishPending() {
        for (PayEventOutbox event : outboxMapper.selectList(new LambdaQueryWrapper<PayEventOutbox>()
                .eq(PayEventOutbox::getPublished, 0).orderByAsc(PayEventOutbox::getId).last("LIMIT 100"))) {
            publishOne(event);
        }
    }

    /**
     * 发送单条并标记已发。定时兜底与「支付事务提交后立即推送」共用这一处，避免两套发送逻辑走歪。
     * <p>
     * 刻意在这里吞掉异常：调用方之一是事务提交后的回调，抛出去会让一笔已经提交成功的支付
     * 向用户报错；而事件仍留在 published=0，下一轮定时任务自然会重试。
     */
    public void publishOne(PayEventOutbox event) {
        try {
            rabbitTemplate.convertAndSend(MqConstants.PAY_NOTIFY_EXCHANGE, MqConstants.PAY_NOTIFY_KEY, event.getBizOrderNo());
            outboxMapper.update(null, new LambdaUpdateWrapper<PayEventOutbox>()
                    .eq(PayEventOutbox::getId, event.getId()).eq(PayEventOutbox::getPublished, 0)
                    .set(PayEventOutbox::getPublished, 1).set(PayEventOutbox::getPublishedTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("支付成功事件发送失败，将重试，orderId={}", event.getBizOrderNo(), e);
        }
    }
}




