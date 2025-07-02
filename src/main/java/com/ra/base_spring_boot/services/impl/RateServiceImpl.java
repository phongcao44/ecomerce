package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ReviewRequest;
import com.ra.base_spring_boot.dto.resp.RatingSummaryResponse;
import com.ra.base_spring_boot.dto.resp.ReviewResponse;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.Review;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.IRateRepository;
import com.ra.base_spring_boot.services.IRateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RateServiceImpl implements IRateService {
    private final IRateRepository reviewRepo;
    private final IProductRepository productRepo;
    private final IUserRepository userRepo;
    private final IOrderRepository orderRepo;
    public  RateServiceImpl(IRateRepository reviewRepo, IProductRepository productRepo, IUserRepository userRepo, IOrderRepository orderRepo) {
        this.reviewRepo = reviewRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
    }
    @Override
    public Double getAverageRatingByProductId(Long productId) {
        List<Review> reviews = reviewRepo.findByProduct_Id(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return Math.round(avg * 10.0) / 10.0;
    }

    @Override
    public void createReview(ReviewRequest request, Long userId) {
        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (orderRepo.countPurchasedProductByUser(userId, product.getId()) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You can't review this product because you haven't purchased it."
            );
        }


        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .product(product)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepo.save(review);
    }

    @Override
    public List<ReviewResponse> getReviewsByProductId(Long productId) {
        return reviewRepo.findByProduct_Id(productId).stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .userName(review.getUser().getUsername())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    @Override
    public RatingSummaryResponse getRatingSummaryByProductId(Long productId) {
        List<Review> reviews = reviewRepo.findByProduct_Id(productId);
        int total = reviews.size();

        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> starMap = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        return RatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .totalReviews(total)
                .starCountMap(starMap)
                .build();
    }



}
