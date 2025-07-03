package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.DistributionCenter;
import com.ra.base_spring_boot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributionCenterRepository extends JpaRepository<DistributionCenter, Integer> {
    Optional<DistributionCenter> findByOrder(Order order);
}
