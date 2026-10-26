package com.campushub.payment.controller;

import com.campushub.common.Result;
import com.campushub.payment.domain.dto.PayFormDTO;
import com.campushub.payment.service.IPayOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final IPayOrderService payOrderService;

    @PostMapping("/orders/{orderId}/pay")
    public Result<Void> payOrder(@PathVariable("orderId") Long orderId,
                                 @RequestBody @Validated PayFormDTO payFormDTO) {
        payOrderService.payOrder(orderId, payFormDTO);
        return Result.success();
    }
}




