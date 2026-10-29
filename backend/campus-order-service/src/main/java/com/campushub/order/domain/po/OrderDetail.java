package com.campushub.order.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_order_detail")
public class OrderDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long itemId;

    /** 卖家用户业务ID（tb_order_detail.seller_id 非空，下单时从商品快照带过来） */
    private Long sellerId;

    /** 以下字段为下单时商品快照 */
    private String name;

    /** 快照单价（分） */
    private Integer price;

    private Integer num;

    private String image;

    private String spec;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}




