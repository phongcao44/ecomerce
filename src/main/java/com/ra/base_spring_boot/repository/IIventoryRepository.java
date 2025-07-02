package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IIventoryRepository extends JpaRepository<ProductVariant, Long> {
}
