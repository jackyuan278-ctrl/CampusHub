package com.campushub.secondhand.domain.vo;

import lombok.Data;

/**
 * 商品 VO，字段名迁就前端契约（categoryName/brand/description）
 */
@Data
public class ItemVO {

    private Long id;

    private String name;

    /** 价格（分） */
    private Integer price;

    private Integer stock;

    private String image;

    private Long categoryId;

    private String categoryName;

    private String brand;

    private String spec;

    private Integer sales;

    /** 1上架 0下架 */
    private Integer status;

    private String description;
}




