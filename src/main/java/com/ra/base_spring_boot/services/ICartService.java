package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;

public interface ICartService {
    CartResponseDTO getUserCart(Long userId);

    CartResponseDTO addItemToCart(Long userId, CartItemRequestDTO request);

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity);

    void removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}
