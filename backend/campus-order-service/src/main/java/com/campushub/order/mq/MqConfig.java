package com.campushub.order.mq;

import com.campushub.api.constants.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ 拓扑：
 * 1. 下单广播：trade.topic -> trade.order.create.queue（预留，暂无消费者）
 * 2. 延迟关单：trade.delay.direct -> trade.order.delay.queue（TTL 30 分钟）
 *    消息过期成为死信 -> trade.dead.direct -> trade.order.dead.queue（监听后关单）
 * 3. 支付通知：pay.topic -> pay.notify.queue（trade 监听后标记已付款）
 */
@Configuration
public class MqConfig {

    @Bean
    public TopicExchange tradeExchange() {
        return new TopicExchange(MqConstants.TRADE_EXCHANGE);
    }

    @Bean
    public DirectExchange tradeDelayExchange() {
        return new DirectExchange(MqConstants.TRADE_DELAY_EXCHANGE);
    }

    @Bean
    public DirectExchange tradeDeadExchange() {
        return new DirectExchange(MqConstants.TRADE_DEAD_EXCHANGE);
    }

    @Bean
    public TopicExchange payExchange() {
        return new TopicExchange(MqConstants.PAY_NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(MqConstants.TRADE_ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(MqConstants.TRADE_ORDER_DELAY_QUEUE)
                .ttl(30 * 60 * 1000)
                .deadLetterExchange(MqConstants.TRADE_DEAD_EXCHANGE)
                .deadLetterRoutingKey(MqConstants.TRADE_ORDER_DEAD_KEY)
                .build();
    }

    @Bean
    public Queue orderDeadQueue() {
        return QueueBuilder.durable(MqConstants.TRADE_ORDER_DEAD_QUEUE).build();
    }

    @Bean
    public Queue payNotifyQueue() {
        return QueueBuilder.durable(MqConstants.PAY_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding orderCreateBinding(Queue orderCreateQueue, TopicExchange tradeExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(tradeExchange).with(MqConstants.TRADE_ORDER_CREATE_KEY);
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange tradeDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(tradeDelayExchange).with(MqConstants.TRADE_ORDER_DELAY_KEY);
    }

    @Bean
    public Binding orderDeadBinding(Queue orderDeadQueue, DirectExchange tradeDeadExchange) {
        return BindingBuilder.bind(orderDeadQueue).to(tradeDeadExchange).with(MqConstants.TRADE_ORDER_DEAD_KEY);
    }

    @Bean
    public Binding payNotifyBinding(Queue payNotifyQueue, TopicExchange payExchange) {
        return BindingBuilder.bind(payNotifyQueue).to(payExchange).with(MqConstants.PAY_NOTIFY_KEY);
    }
}




