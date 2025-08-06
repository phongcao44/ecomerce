package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.model.ProductVariant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IFlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {
    @EntityGraph(attributePaths = {"product", "variant"})
    List<FlashSaleItem> findByFlashSaleId(Long id);

    FlashSaleItem findByVariant(ProductVariant variant);

    FlashSaleItem findTopByOrderByDiscountedPriceDesc();


    @Query(value = """
    SELECT fsi.* FROM flash_sales_items fsi
    JOIN product_variants pv ON fsi.variant_id = pv.id
    WHERE fsi.flash_sale_id = :flashSaleId
    ORDER BY (pv.price_override - fsi.discounted_price) DESC
    LIMIT 1
""", nativeQuery = true)
    FlashSaleItem findTopDiscountItemByFlashSaleId(@Param("flashSaleId") Long flashSaleId);

    // Trong FlashSaleItemRepository
    boolean existsByFlashSaleIdAndVariantId(Long flashSaleId, Long variantId);

    boolean existsByFlashSaleIdAndVariantIdAndIdNot(Long flashSaleId, Long variantId, Long id);

}

