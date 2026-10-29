package com.campushub.cart.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCartDTO {

    @NotNull(message = "商品id不能为空")
    private Long itemId;

    /** 二手商品一物一件，数量恒为 1；字段保留是为了与订单明细结构对齐 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 1, message = "二手商品一物一件，每次只能加购1件")
    private Integer num;
}




