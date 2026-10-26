package com.campushub.cart.controller;

import com.campushub.cart.domain.dto.AddCartDTO;
import com.campushub.cart.domain.dto.UpdateCartNumDTO;
import com.campushub.cart.domain.vo.CartItemVO;
import com.campushub.cart.service.ICartService;
import com.campushub.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @GetMapping
    public Result<List<CartItemVO>> queryMyCart() {
        return Result.success(cartService.queryMyCart());
    }

    @PostMapping
    public Result<Void> addItem(@RequestBody @Validated AddCartDTO addCartDTO) {
        cartService.addItem(addCartDTO);
        return Result.success();
    }

    @PutMapping("/{itemId}")
    public Result<Void> updateNum(@PathVariable("itemId") Long itemId,
                                  @RequestBody @Validated UpdateCartNumDTO updateCartNumDTO) {
        cartService.updateNum(itemId, updateCartNumDTO);
        return Result.success();
    }

    @DeleteMapping("/{itemId}")
    public Result<Void> removeItem(@PathVariable("itemId") Long itemId) {
        cartService.removeItem(itemId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clearCart() {
        cartService.clearCart();
        return Result.success();
    }
}




