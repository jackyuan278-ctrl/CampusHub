package com.campushub.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 余额扣款幂等流水：一个业务订单号最多产生一笔扣款。 */
@Data
@TableName("tb_balance_deduction")
public class BalanceDeduction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bizOrderNo;

    private Long userId;

    /** 金额单位：分 */
    private Integer amount;

    private LocalDateTime createTime;
}




