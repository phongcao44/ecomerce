package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Cart;
import com.ra.base_spring_boot.model.CartItem;
import com.ra.base_spring_boot.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ICartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByCart(Cart cart);

    List<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);

    void deleteByCart(Cart cart);

    List<CartItem> findAllByCart_User_Id(Long userId);

    // Đếm số item trong giỏ hàng của user
    long countByCart_User_Id(Long userId);

    //  kiểm tra true/false :
    boolean existsByCart_User_Id(Long userId);
}
