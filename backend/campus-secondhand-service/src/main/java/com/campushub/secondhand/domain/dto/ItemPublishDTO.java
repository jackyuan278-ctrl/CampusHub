package com.campushub.secondhand.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布商品入参。
 * <p>
 * 刻意不含 sellerId / status / id：卖家身份取自网关注入的 user-info，
 * 状态由服务端固定为「在售」，一律不采信前端传值。
 */
@Data
public class ItemPublishDTO {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 255, message = "商品名称不能超过 255 字")
    private String name;

    /** 价格（分） */
    @NotNull(message = "价格不能为空")
    @Min(value = 1, message = "价格必须大于 0")
    @Max(value = 10_000_000, message = "价格不能超过 10 万元")
    private Integer price;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Size(max = 500, message = "图片地址过长")
    private String image;

    @Size(max = 100, message = "成色描述不能超过 100 字")
    private String conditionDesc;

    @Size(max = 1000, message = "商品描述不能超过 1000 字")
    private String description;

    @Size(max = 255, message = "交易地点不能超过 255 字")
    private String tradeLocation;
}
