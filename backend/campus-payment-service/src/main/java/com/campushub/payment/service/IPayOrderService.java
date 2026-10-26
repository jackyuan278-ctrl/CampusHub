package com.campushub.paymentment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.paymentment.domain.dto.PayFormDTO;
import com.campushub.paymentment.domain.po.PayOrder;

public interface IPayOrderService extends IService<PayOrder> {

    void payOrder(Long orderId, PayFormDTO payFormDTO);
}




