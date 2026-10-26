package com.campushub.paymentment.domain.po;

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
@TableName("tb_pay_order")
public class PayOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务订单号（trade 服务订单 id），唯一键保证一单一支付单 */
    private Long bizOrderNo;

    private Long payUserId;

    /** 支付金额（分） */
    private Integer amount;

    /** 支付状态：1待支付、2已支付、3处理中。处理中用于阻止并发支付请求重复扣款。 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime updateTime;
}




