package com.campushub.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.payment.domain.dto.PayFormDTO;
import com.campushub.payment.domain.po.PayOrder;

public interface IPayOrderService extends IService<PayOrder> {

    void payOrder(Long orderId, PayFormDTO payFormDTO);
}




