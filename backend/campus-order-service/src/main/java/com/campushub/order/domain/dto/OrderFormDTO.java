package com.campushub.order.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class OrderFormDTO {

    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    /** 备注（DB 暂无此列，需加列或砍掉，待拍板） */
    @Size(max = 255, message = "备注过长")
    private String remark;

    @NotEmpty(message = "下单商品不能为空")
    private List<@NotNull(message = "商品明细不能为空") @Valid OrderGoodsDTO> goods;
}




