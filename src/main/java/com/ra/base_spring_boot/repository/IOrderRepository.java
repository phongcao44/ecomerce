package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrderRepository extends JpaRepository<Order, Long> {
}
