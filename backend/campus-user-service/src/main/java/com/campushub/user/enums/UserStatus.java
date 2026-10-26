package com.campushub.user.enums;

import com.campushub.common.BizException;
import lombok.Getter;

@Getter
public enum UserStatus {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final int value;
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static UserStatus of(Integer value) {
        if (value == null) {
            return NORMAL;
        }
        for (UserStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new BizException(400, "非法的用户状态: " + value);
    }
}




