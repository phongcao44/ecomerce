package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<Product> findTop4ByCategoryAndIdNot(Category category, Long id);
}
