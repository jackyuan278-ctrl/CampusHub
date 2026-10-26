package com.campushub.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.order.domain.po.OrderDetail;
import com.campushub.order.mapper.OrderDetailMapper;
import com.campushub.order.service.IOrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements IOrderDetailService {
}




