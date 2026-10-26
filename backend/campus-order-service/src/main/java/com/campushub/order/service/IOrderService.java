package com.campushub.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.order.domain.dto.OrderFormDTO;
import com.campushub.order.domain.po.Order;
import com.campushub.order.domain.vo.OrderVO;

import java.util.List;

public interface IOrderService extends IService<Order> {

    OrderVO createOrder(OrderFormDTO orderFormDTO);

    List<OrderVO> queryMyOrders();

    OrderVO queryOrderById(Long id);

    /** 支付成功回调（MQ pay.success 监听器调用）：订单 1 -> 2，写 pay_time */
    void markPaySuccess(Long orderId);

    /** 超时关单（MQ 死信监听器调用）：仅关 status=1 的订单，并回滚库存 */
    void closeOrder(Long orderId);
}




