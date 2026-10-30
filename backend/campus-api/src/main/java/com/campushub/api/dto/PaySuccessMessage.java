package com.campushub.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付成功事件的载荷。
 * <p>
 * 原来是裸的订单号，交易服务只能把订单改成「已付款」，不知道该写哪种支付方式，
 * 导致 tb_order.payment_type 永远停在「未设置」。把渠道一起带上，由交易服务
 * 在同一个条件更新里回写，订单详情页才有「余额支付 / 模拟支付」可显示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaySuccessMessage {

    private Long orderId;

    /** 实际支付渠道：balance / mock，与前端 payType 取值一致 */
    private String channel;
}
