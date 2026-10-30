package com.campushub.order.mq;

import com.campushub.api.constants.MqConstants;
import com.campushub.api.dto.PaySuccessMessage;
import com.campushub.order.service.IOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeMessageListener {

    private final IOrderService orderService;
    private final ObjectMapper objectMapper;

    /** 支付成功通知（pay 服务发送）：订单 1 -> 2，并回写实际支付方式 */
    @RabbitListener(queues = MqConstants.PAY_NOTIFY_QUEUE)
    public void onPaySuccess(String payload) {
        PaySuccessMessage message = parsePaySuccess(payload);
        if (message == null) {
            return;
        }
        log.info("收到支付成功通知，orderId={}, channel={}", message.getOrderId(), message.getChannel());
        orderService.markPaySuccess(message.getOrderId(), message.getChannel());
    }

    /**
     * 解析支付成功事件。
     * <p>
     * 兼容旧的裸订单号格式：滚动发布时队列里可能还压着上一条格式的消息。
     * 解析不出来的是不可恢复的编码错误，重投多少次都一样，记 error 后丢弃，避免消息无限重投把日志刷满。
     */
    private PaySuccessMessage parsePaySuccess(String payload) {
        if (payload == null || payload.isBlank()) {
            log.error("支付成功通知载荷为空，丢弃");
            return null;
        }
        String trimmed = payload.trim();
        if (trimmed.matches("\\d+")) {
            return new PaySuccessMessage(Long.valueOf(trimmed), null);
        }
        try {
            return objectMapper.readValue(trimmed, PaySuccessMessage.class);
        } catch (Exception e) {
            log.error("支付成功通知载荷无法解析，丢弃，payload={}", trimmed, e);
            return null;
        }
    }

    /** 延迟队列死信：30 分钟未支付，超时关单并回滚库存 */
    @RabbitListener(queues = MqConstants.TRADE_ORDER_DEAD_QUEUE)
    public void onOrderDead(Long orderId) {
        log.info("收到超时关单通知，orderId={}", orderId);
        orderService.closeOrder(orderId);
    }
}
