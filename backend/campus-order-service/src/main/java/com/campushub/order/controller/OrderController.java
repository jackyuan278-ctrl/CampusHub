package com.campushub.order.controller;

import com.campushub.common.Result;
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
    public Result<List<OrderVO>> queryMyOrders() {
        return Result.success(orderService.queryMyOrders());
    }

    @GetMapping("/{id}")
    public Result<OrderVO> queryOrderById(@PathVariable("id") Long id) {
        return Result.success(orderService.queryOrderById(id));
    }
}




