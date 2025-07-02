package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IRateRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_Id(Long productId);
}
