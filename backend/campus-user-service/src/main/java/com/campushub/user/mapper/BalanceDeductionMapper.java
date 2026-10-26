package com.campushub.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.user.domain.po.BalanceDeduction;
import org.apache.ibatis.annotations.Select;

public interface BalanceDeductionMapper extends BaseMapper<BalanceDeduction> {

    /** 锁定读取，用于在重复请求时确认幂等流水及其用户、金额。 */
    @Select("SELECT id, biz_order_no, user_id, amount, create_time FROM tb_balance_deduction WHERE biz_order_no = #{bizOrderNo} FOR UPDATE")
    BalanceDeduction selectByBizOrderNoForUpdate(Long bizOrderNo);
}




