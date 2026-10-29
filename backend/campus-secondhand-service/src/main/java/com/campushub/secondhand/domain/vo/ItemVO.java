package com.campushub.secondhand.domain.vo;

import lombok.Data;

/**
 * 商品 VO，字段迁就前端契约（categoryName/conditionDesc/tradeLocation/sellerId）
 */
@Data
public class ItemVO {

    private Long id;

    /** 卖家业务ID */
    private Long sellerId;

    private String name;

    /** 价格（分） */
    private Integer price;

    private String image;

    private Long categoryId;

    private String categoryName;

    /** 成色描述 */
    private String conditionDesc;

    /** 交易地点 */
    private String tradeLocation;

    /** 0下架 1在售 2预订 3已售 */
    private Integer status;

    private String description;
}




