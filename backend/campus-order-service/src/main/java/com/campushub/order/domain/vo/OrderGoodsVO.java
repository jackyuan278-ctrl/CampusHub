package com.campushub.order.domain.vo;

import lombok.Data;

@Data
public class OrderGoodsVO {

    private Long itemId;

    private String name;

    /** 快照单价（分） */
    private Integer price;

    private Integer num;

    private String image;

    private String spec;
}




