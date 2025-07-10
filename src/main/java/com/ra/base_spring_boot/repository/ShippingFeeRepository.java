package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.ShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingFeeRepository extends JpaRepository<ShippingFee, Long> {
    Optional<ShippingFee> findByOrder(Order order);
}
