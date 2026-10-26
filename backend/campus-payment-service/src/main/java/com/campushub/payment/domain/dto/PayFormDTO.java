package com.campushub.payment.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayFormDTO {

    /** balance 余额 / mock 模拟扫码（对应订单 payment_type 1/2） */
    @NotBlank(message = "支付方式不能为空")
    private String payType;
}




