package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.req.OrderRequestAllDTO;
import com.ra.base_spring_boot.dto.req.OrderRequestSelectedDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;
import com.ra.base_spring_boot.dto.resp.OrderCheckoutResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ICartService {

    CartResponseDTO getUserCart(Long userId);

    CartResponseDTO addItemToCart(Long userId, CartItemRequestDTO request);

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity);

    void removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);

    OrderCheckoutResponseDTO checkout(Long userId, OrderRequestAllDTO request);

    OrderCheckoutResponseDTO checkoutSelectedItems(Long userId, OrderRequestSelectedDTO request);

    // OrderCheckoutResponseDTO checkoutByCartItemId(Long userId, Long cartItemId, OrderRequestSelectedDTO request);

    BigDecimal getCartTotal(Long userId);

    List<Long> getUsersWithCartItems();
}
