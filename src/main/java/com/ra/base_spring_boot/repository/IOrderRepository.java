package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IOrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findAllByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);
    List<Order> findAllByStatus(OrderStatus status);
    List<Order> findOrdersByPaymentMethod(PaymentMethod PaymentMethod);
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

    long countByStatus(OrderStatus status);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);


    @Query("""
SELECT DISTINCT v.product.id
FROM Order o
JOIN o.orderItems oi
JOIN oi.variant v
WHERE o.user.id = :userId
  AND o.status = com.ra.base_spring_boot.model.constants.OrderStatus.DELIVERED
""")
    List<Long> findPurchasedProductIdsByUser(@Param("userId") Long userId);


}
