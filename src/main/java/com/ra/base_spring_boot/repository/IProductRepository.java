package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IProductRepository extends JpaRepository<Product, Long> {
    @Query("""
    SELECT v.product, count(oi.id) as purchaseCount
        from Order o
            join o.orderItems oi
                join oi.variant v
                    where o.status = com.ra.base_spring_boot.model.constants.OrderStatus.DELIVERED
                        GROUP BY v.product
                            ORDER BY purchaseCount DESC
    """)
    List<Object[]> findTop5BestSellingProducts(Pageable pageable);

    List<Product> findProductByNameContainsIgnoreCase(String name);

    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    @Query("""
    SELECT p, COUNT(oi)
    FROM Product p
    LEFT JOIN ProductVariant v ON v.product = p
    LEFT JOIN OrderItem oi ON oi.variant = v
    LEFT JOIN Order o ON o = oi.order AND o.status = com.ra.base_spring_boot.model.constants.OrderStatus.DELIVERED
    GROUP BY p
    ORDER BY COUNT(oi.id) ASC
""")
    List<Object[]> findTop5LeastSellingOrUnsoldProducts(Pageable pageable);

}
