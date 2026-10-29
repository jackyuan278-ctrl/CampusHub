package com.campushub.order.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 前端下单商品条目（快照仅展示，服务端以商品服务实时价重算金额）
 */
@Data
public class OrderGoodsDTO {

    @NotNull(message = "商品id不能为空")
    private Long itemId;

    private String name;

    private Integer price;

    /** 二手商品一物一件，数量恒为 1 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 1, message = "二手商品一物一件，每单只能购买1件")
    private Integer num;

    private String spec;

    private String image;
}




