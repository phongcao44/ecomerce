package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("""
                SELECT v.product, COUNT(oi.id) as purchaseCount,
                       COALESCE(AVG(r.rating), 0), COUNT(r.id)
                FROM Order o
                    JOIN o.orderItems oi
                    JOIN oi.variant v
                    LEFT JOIN Review r ON r.product = v.product
                WHERE o.status = :status
                GROUP BY v.product
                ORDER BY purchaseCount DESC
            """)
    List<Object[]> findTop5BestSellingProducts(@Param("status") OrderStatus status, Pageable pageable);

    List<Product> findProductByNameContainsIgnoreCase(String name);

    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    Optional<Product> findByNameIgnoreCase(String name);

    @Query("""
                SELECT p, 
                       COUNT(oi.id), 
                       COALESCE(AVG(r.rating), 0), 
                       COUNT(DISTINCT pv.id)
                FROM Product p
                LEFT JOIN ProductVariant v ON v.product = p
                LEFT JOIN OrderItem oi ON oi.variant = v
                LEFT JOIN Order o ON oi.order = o AND o.status = :status
                LEFT JOIN Review r ON r.product = p
                LEFT JOIN ProductView pv ON pv.product = p
                GROUP BY p
                ORDER BY COUNT(oi.id) ASC
            """)
    List<Object[]> findTop5LeastSellingWithRatingAndView(@Param("status") OrderStatus status, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status, Pageable pageable);

    @Query(value = """
    SELECT p.*, 
           SUM(od.quantity) AS totalSold,
           AVG(r.rating) AS averageRating,
           COUNT(r.id) AS totalReviews
    FROM products p
    JOIN product_variants pv ON pv.product_id = p.id
    JOIN order_details od ON od.variant_id = pv.id
    JOIN orders o ON o.id = od.order_id
    LEFT JOIN reviews r ON r.product_id = p.id
    WHERE o.status = 'DELIVERED'
      AND (:brandName IS NULL OR p.brand = :brandName)
      AND (:categoryId IS NULL OR p.category_id = :categoryId)
      AND (:priceMin IS NULL OR pv.price_override >= :priceMin)
      AND (:priceMax IS NULL OR pv.price_override <= :priceMax)
    GROUP BY p.id
    ORDER BY 
        CASE WHEN :sortBy = 'totalSold' THEN SUM(od.quantity)
             WHEN :sortBy = 'totalRevenue' THEN SUM(od.quantity * od.price)
             WHEN :sortBy = 'totalQuantity' THEN COUNT(od.id)
             WHEN :sortBy = 'averageRating' THEN AVG(r.rating)
             WHEN :sortBy = 'price' THEN MIN(pv.price_override)
             WHEN :sortBy = 'createdAt' THEN p.created_at
        END
        COLLATE utf8mb4_unicode_ci 
        COLLATE utf8mb4_unicode_ci
        :orderBy
    LIMIT :limit OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findFilteredBestSellers(
            @Param("brandName") String brandName,
            @Param("categoryId") Long categoryId,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            @Param("sortBy") String sortBy,
            @Param("orderBy") String orderBy,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

}
