package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteOrderItemByOrderId(Long orderId);
    List<OrderItem> findByVariantIdIn(List<Long> variantIds);

    @Query("""
    SELECT SUM(oi.quantity)
    FROM OrderItem oi
    WHERE oi.variant.product.id = :productId
      AND oi.order.status = 'DELIVERED'
""")
    Integer countSoldQuantityByProductId(@Param("productId") Long productId);
}
