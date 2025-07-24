package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductImage;
import com.ra.base_spring_boot.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductId(Long productId);

    List<ProductImage> findByProduct_NameIgnoreCase(String name);

    List<ProductImage> findByVariantId(Long variantId);

    Optional<ProductImage> findFirstByProductAndIsMainTrue(Product product);

}
