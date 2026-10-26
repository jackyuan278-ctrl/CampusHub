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
}




