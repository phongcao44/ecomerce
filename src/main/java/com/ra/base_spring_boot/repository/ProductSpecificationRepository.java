package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    List<ProductSpecification> findAllByProduct(Product product);

    List<ProductSpecification> findAllByProduct_Id(Long productId);
}
