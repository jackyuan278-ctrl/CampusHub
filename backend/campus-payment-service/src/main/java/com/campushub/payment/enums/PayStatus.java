package com.campushub.paymentment.enums;

import com.campushub.common.BizException;
import lombok.Getter;

@Getter
public enum PayStatus {

    UNPAID(1, "未支付"),
    PAID(2, "已支付");

    private final int value;
    private final String desc;

    PayStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static PayStatus of(Integer value) {
        if (value == null) {
            return UNPAID;
        }
        for (PayStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new BizException(400, "非法的支付状态: " + value);
    }
}




