package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IRateRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_Id(Long productId);
    @Query("SELECT DISTINCT r.product.id FROM Review r WHERE r.user.id = :userId")
    List<Long> findReviewedProductIdsByUser(@Param("userId") Long userId);
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    void deleteReviewByProductId(Long productId);
}
