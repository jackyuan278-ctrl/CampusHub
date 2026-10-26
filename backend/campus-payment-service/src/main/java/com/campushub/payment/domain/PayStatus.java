package com.campushub.payment.domain;

/** 支付单状态。状态变更通过数据库条件更新完成，避免并发请求重复执行支付。 */
public final class PayStatus {
    public static final int UNPAID = 1;
    public static final int PAID = 2;
    public static final int PROCESSING = 3;

    private PayStatus() {
    }
}




