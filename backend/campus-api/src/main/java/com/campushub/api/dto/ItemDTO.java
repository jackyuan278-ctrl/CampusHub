package com.campushub.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemDTO {

    private Long id;
    /** 卖家用户业务ID：下单时写入订单明细快照（tb_order_detail.seller_id 非空） */
    private Long sellerId;
    private String name;
    private Integer price;
    private Integer stock;
    private String image;
    /** 商品描述（详情页展示 + AI 导购 RAG 素材） */
    private String description;
    private Long categoryId;
    private Long brandId;
    private String spec;
    private Integer sales;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}




