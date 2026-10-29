package com.campushub.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务号生成器。
 *
 * <p>订单号、支付单号这类跨服务标识必须是业务系统自己生成的可读字符串，
 * 不能依赖数据库自增，也不能直接复用雪花 ID（长度、可读性、泄露写入量都不合适）。
 *
 * <p>格式：前缀 + yyyyMMddHHmmss + 6 位随机数，例如 {@code O20261028233107482130}。
 * 随机后缀只用来降低同一秒内的碰撞概率，真正的唯一性由表上的唯一索引兜底。
 */
public final class BizNoGenerator {

    private static final DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RANDOM_BOUND = 1_000_000;

    private BizNoGenerator() {
    }

    /** 订单业务号，对应 tb_order.order_no */
    public static String order() {
        return next("O");
    }

    /** 支付业务号，对应 tb_pay_order.payment_no / tb_pay_event_outbox.payment_no */
    public static String payment() {
        return next("P");
    }

    private static String next(String prefix) {
        return prefix
                + LocalDateTime.now().format(SECOND_FORMATTER)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(RANDOM_BOUND));
    }
}
