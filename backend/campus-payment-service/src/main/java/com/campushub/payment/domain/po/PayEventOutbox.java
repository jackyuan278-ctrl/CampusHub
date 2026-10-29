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
    private Integer published;
    private LocalDateTime createTime;
    private LocalDateTime publishedTime;
}




