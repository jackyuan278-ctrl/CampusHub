package com.campushub.payment.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** Durable event record so a committed payment can still notify Trade after a restart. */
@Data
@TableName("tb_pay_event_outbox")
public class PayEventOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付业务号（tb_pay_event_outbox.payment_no 非空，与 event_type 组成唯一键） */
    private String paymentNo;
    private Long bizOrderNo;
    /** 实际支付渠道，随事件一起投递，交易服务据此回写订单的支付方式 */
    private String channel;
    private Integer published;
    private LocalDateTime createTime;
    private LocalDateTime publishedTime;
}




