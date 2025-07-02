package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.req.OrderRequestDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;
import com.ra.base_spring_boot.dto.resp.OrderCheckoutResponseDTO;

import java.math.BigDecimal;

public interface ICartService {

    CartResponseDTO getUserCart(Long userId);

    CartResponseDTO addItemToCart(Long userId, CartItemRequestDTO request);

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity);

    void removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);

    OrderCheckoutResponseDTO checkout(Long userId, OrderRequestDTO request);

    OrderCheckoutResponseDTO checkoutByCartItemId(Long userId, Long cartItemId, OrderRequestDTO request);

    BigDecimal getCartTotal(Long userId);
}
