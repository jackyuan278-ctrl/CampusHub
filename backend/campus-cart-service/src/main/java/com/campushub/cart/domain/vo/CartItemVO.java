package com.campushub.cart.domain.vo;

import lombok.Data;

/**
 * 购物车条目（加购时商品快照，快照价仅展示，结算以商品服务实时价为准）
 */
@Data
public class CartItemVO {

    private Long itemId;

    private String name;

    /** 快照价（分） */
    private Integer price;

    private String image;

    private String spec;

    private Integer num;
}




