package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByProduct(Product product);
}

