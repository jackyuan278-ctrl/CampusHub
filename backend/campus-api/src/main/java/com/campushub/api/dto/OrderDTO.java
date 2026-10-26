package com.campushub.api.dto;

import lombok.Data;

/**
 * trade 服务订单摘要（pay 核价/核状态用），字段为 OrderVO 子集，反序列化忽略多余字段
 */
@Data
public class OrderDTO {

    private Long id;

    /** 订单总金额（分） */
    private Integer totalFee;

    /** 1未付款 2已付款 3已发货 4已完成 5已关闭 */
    private Integer status;

    /** 1余额 2模拟支付 */
    private Integer paymentType;
}




