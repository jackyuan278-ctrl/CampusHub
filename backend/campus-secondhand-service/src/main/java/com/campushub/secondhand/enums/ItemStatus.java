package com.campushub.secondhand.enums;

import com.campushub.common.BizException;
import lombok.Getter;

@Getter
public enum ItemStatus {

    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "在售"),
    RESERVED(2, "预订"),
    SOLD(3, "已售");

    private static final String INVALID_STATUS_MESSAGE = "非法的商品状态: ";

    private final int value;
    private final String desc;

    ItemStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ItemStatus of(Integer value) {
        if (value == null) {
            return ON_SHELF;
        }
        for (ItemStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new BizException(BizException.BAD_REQUEST_CODE, INVALID_STATUS_MESSAGE + value);
    }
}




