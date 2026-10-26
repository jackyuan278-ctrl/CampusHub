package com.campushub.api.constants;

public interface MqConstants {

    String TRADE_EXCHANGE = "trade.topic";
    String TRADE_DELAY_EXCHANGE = "trade.delay.direct";
    String TRADE_DEAD_EXCHANGE = "trade.dead.direct";

    String TRADE_ORDER_CREATE_QUEUE = "trade.order.create.queue";
    String TRADE_ORDER_DELAY_QUEUE = "trade.order.delay.queue";
    String TRADE_ORDER_DEAD_QUEUE = "trade.order.dead.queue";

    String TRADE_ORDER_CREATE_KEY = "order.create";
    String TRADE_ORDER_DELAY_KEY = "order.delay";
    String TRADE_ORDER_DEAD_KEY = "order.dead";

    String PAY_NOTIFY_EXCHANGE = "pay.topic";
    String PAY_NOTIFY_QUEUE = "pay.notify.queue";
    String PAY_NOTIFY_KEY = "pay.success";
}




