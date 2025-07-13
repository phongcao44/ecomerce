package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.model.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IFlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {
    @EntityGraph(attributePaths = {"product", "variant"})
    List<FlashSaleItem> findByFlashSaleId(Long id);
    FlashSaleItem findByVariant(ProductVariant variant);

}
