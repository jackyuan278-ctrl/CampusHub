package com.campushub.order.mq;

import com.campushub.api.constants.MqConstants;
import com.campushub.order.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeMessageListener {

    private final IOrderService orderService;

    /** 支付成功通知（pay 服务发送）：订单 1 -> 2 */
    @RabbitListener(queues = MqConstants.PAY_NOTIFY_QUEUE)
    public void onPaySuccess(Long orderId) {
        log.info("收到支付成功通知，orderId={}", orderId);
        orderService.markPaySuccess(orderId);
    }

    /** 延迟队列死信：30 分钟未支付，超时关单并回滚库存 */
    @RabbitListener(queues = MqConstants.TRADE_ORDER_DEAD_QUEUE)
    public void onOrderDead(Long orderId) {
        log.info("收到超时关单通知，orderId={}", orderId);
        orderService.closeOrder(orderId);
    }
}




