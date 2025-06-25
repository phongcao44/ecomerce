package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IOrderRepository extends JpaRepository<Order, Long> {
    @Query("""
    SELECT COUNT(oi) FROM Order o
    JOIN o.orderItems oi
    JOIN oi.variant v
    WHERE o.user.id = :userId
      AND v.product.id = :productId
      AND o.status = com.ra.base_spring_boot.model.constants.OrderStatus.DELIVERED
""")
    Long countPurchasedProductByUser(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );

