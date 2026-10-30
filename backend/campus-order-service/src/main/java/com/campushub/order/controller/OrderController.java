package com.campushub.order.controller;

import com.campushub.common.Result;
import com.campushub.common.domain.PageDTO;
import com.campushub.order.domain.dto.OrderFormDTO;
import com.campushub.order.domain.vo.OrderVO;
import com.campushub.order.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    public Result<OrderVO> createOrder(@RequestBody @Validated OrderFormDTO orderFormDTO) {
        return Result.success(orderService.createOrder(orderFormDTO));
    }

    @GetMapping
    public Result<PageDTO<OrderVO>> queryMyOrders(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                  @RequestParam(value = "status", required = false) Integer status) {
        return Result.success(orderService.queryMyOrders(page, pageSize, status));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> queryOrderById(@PathVariable("id") Long id) {
        return Result.success(orderService.queryOrderById(id));
    }

    /** 待支付订单可主动取消，等同把超时关单提前执行一次 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable("id") Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }
}




