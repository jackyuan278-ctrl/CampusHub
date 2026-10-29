package com.campushub.cart.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartNumDTO {

    /** 二手商品一物一件，数量恒为 1；传其它值会被参数校验直接拒绝 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 1, message = "二手商品一物一件，数量不可修改")
    private Integer num;
}




