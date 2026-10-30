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

    /**
     * 商品当前状态：0下架 1在售 2预订 3已售。
     * 不入 Redis 快照，每次读购物车时现查现填；查不到时留 null（服务降级，前端不标灰）
     */
    private Integer status;
}




