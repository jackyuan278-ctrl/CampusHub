package com.campushub.order.enums;

import com.campushub.common.BizException;
import lombok.Getter;

@Getter
public enum OrderStatus {

    UNPAID(1, "未付款"),
    PAID(2, "已付款"),
    SHIPPED(3, "已发货"),
    FINISHED(4, "已完成"),
    CLOSED(5, "已关闭");

    private final int value;
    private final String desc;

    OrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static OrderStatus of(Integer value) {
        if (value == null) {
            return UNPAID;
        }
        for (OrderStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new BizException(400, "非法的订单状态: " + value);
    }
}




