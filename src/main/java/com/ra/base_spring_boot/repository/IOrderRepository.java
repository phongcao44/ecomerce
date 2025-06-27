package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IOrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);
    List<Order> findAllByStatus(OrderStatus status);
}
