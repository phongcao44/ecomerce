package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IProductViewRepository extends JpaRepository<ProductView, Long> {
    // Lượt xem gần nhất theo product và session
    Optional<ProductView> findTopByProduct_IdAndSessionIdOrderByViewedAtDesc(Long productId, String sessionId);
    //chan spam
    Optional<ProductView> findTopByProduct_IdAndUser_IdOrderByViewedAtDesc(Long productId, Long userId);

}
