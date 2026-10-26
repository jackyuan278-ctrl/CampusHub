package com.campushub.paymentment.domain.po;

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
    private Long bizOrderNo;
    private Integer published;
    private LocalDateTime createTime;
    private LocalDateTime publishedTime;
}




