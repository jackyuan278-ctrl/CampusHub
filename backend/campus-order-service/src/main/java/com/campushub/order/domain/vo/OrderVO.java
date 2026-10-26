package com.campushub.order.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;

    /** 订单总金额（分） */
    private Integer totalFee;

    /** 1余额 2模拟支付 */
    private Integer paymentType;

    /** 1未付款 2已付款 3已发货 4已完成 5已关闭 */
    private Integer status;

    private String receiver;

    private String phone;

    private String address;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private List<OrderGoodsVO> goods;
}




