package com.campushub.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品轻量状态快照：只带购物车/下单这类场景需要的字段，
 * 避免为了看一个 status 把整条商品记录搬来搬去。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemStatusDTO {

    private Long id;

    /** 0下架 1在售 2预订 3已售 */
    private Integer status;

    /** 实时价（分），结算时以它为准 */
    private Integer price;
}
