package com.campushub.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.common.domain.PageDTO;
import com.campushub.order.domain.dto.OrderFormDTO;
import com.campushub.order.domain.po.Order;
import com.campushub.order.domain.vo.OrderVO;

import java.util.List;

public interface IOrderService extends IService<Order> {

    OrderVO createOrder(OrderFormDTO orderFormDTO);

    /** 分页查我的订单（按创建时间倒序），status 为 null 表示全部 */
    PageDTO<OrderVO> queryMyOrders(Integer page, Integer pageSize, Integer status);

    OrderVO queryOrderById(Long id);

    /** 用户主动取消：只有待支付的订单能取消，复用关单逻辑回滚库存 */
    void cancelOrder(Long id);

    /** 支付成功回调（MQ pay.success 监听器调用）：订单 1 -> 2，写 pay_time */
    void markPaySuccess(Long orderId);

    /** 超时关单（MQ 死信监听器调用）：仅关 status=1 的订单，并回滚库存 */
    void closeOrder(Long orderId);
}




