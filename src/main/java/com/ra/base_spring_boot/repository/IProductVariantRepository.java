package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);

    Long Id(Long id);
}
