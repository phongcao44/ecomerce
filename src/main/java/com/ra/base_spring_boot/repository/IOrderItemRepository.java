package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteOrderItemByOrderId(Long orderId);
}
