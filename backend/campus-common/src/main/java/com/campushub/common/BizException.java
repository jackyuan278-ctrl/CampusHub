package com.campushub.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    public static final int BAD_REQUEST_CODE = 400;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int CONFLICT_CODE = 409;
    public static final int INTERNAL_SERVER_ERROR_CODE = 500;

    private final Integer code;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(INTERNAL_SERVER_ERROR_CODE, message);
    }
}




