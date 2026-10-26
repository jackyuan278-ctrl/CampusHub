package com.campushub.payment.mq;

import com.campushub.api.constants.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付侧 MQ 拓扑声明（与 trade 侧同名，重复声明幂等安全）：
 * pay.topic --pay.success--> pay.notify.queue（trade 消费，订单 1->2）
 */
@Configuration
public class MqConfig {

    @Bean
    public TopicExchange payExchange() {
        return new TopicExchange(MqConstants.PAY_NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue payNotifyQueue() {
        return QueueBuilder.durable(MqConstants.PAY_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding payNotifyBinding(Queue payNotifyQueue, TopicExchange payExchange) {
        return BindingBuilder.bind(payNotifyQueue).to(payExchange).with(MqConstants.PAY_NOTIFY_KEY);
    }
}




