package com.hmall.trade.constants;

public interface MQConstants {
    String DELAY_EXCHANGE_NAME = "trade.delay.direct";
    String DELAY_ORDER_QUEUE_NAME = "trade.delay.order.queue";
    int   DELAY_ORDER_TIMEOUT = 60 * 1000;
    String DELAY_ORDER_KEY = "trade.order.query";
}
