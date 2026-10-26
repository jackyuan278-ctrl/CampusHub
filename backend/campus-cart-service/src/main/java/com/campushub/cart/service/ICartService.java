package com.campushub.cart.service;

import com.campushub.cart.domain.dto.AddCartDTO;
import com.campushub.cart.domain.dto.UpdateCartNumDTO;
import com.campushub.cart.domain.vo.CartItemVO;

import java.util.List;

public interface ICartService {

    /** 我的购物车列表（Redis Hash: campushub:cart:{userId}，field=itemId，value=快照JSON） */
    List<CartItemVO> queryMyCart();

    /** 加购：已存在则数量累加，数量上限 99 */
    void addItem(AddCartDTO addCartDTO);

    void updateNum(Long itemId, UpdateCartNumDTO updateCartNumDTO);

    void removeItem(Long itemId);

    void clearCart();
}




