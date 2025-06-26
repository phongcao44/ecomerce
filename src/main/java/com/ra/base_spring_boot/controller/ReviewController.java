package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.req.ReviewRequest;
import com.ra.base_spring_boot.dto.resp.RatingSummaryResponse;
import com.ra.base_spring_boot.dto.resp.ReviewResponse;
import com.ra.base_spring_boot.repository.RateRepository;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.RateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final RateService reviewService;
    private final RateRepository rateRepository;


    @PostMapping
    public ResponseEntity<?> createReview(@Valid @RequestBody ReviewRequest request,
                                          @AuthenticationPrincipal MyUserDetails user) {
        reviewService.createReview(request, user.getUser().getId());
        return ResponseEntity.ok("Review created successfully.");
    }
    @GetMapping("/user/review/product/{productId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long productId) {
        Double avgRating = reviewService.getAverageRatingByProductId(productId);
        return ResponseEntity.ok(avgRating);
    }

    @GetMapping("/user/review/product/{productId}")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }
    @GetMapping("/user/review/product/{productId}/rating-summary")
    public ResponseEntity<RatingSummaryResponse> getRatingSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getRatingSummaryByProductId(productId));
    }

    //huynh gia phuc
    @GetMapping("/admin/review/list/{id}")
    public ResponseEntity<?> getReviews(@PathVariable Long id){
        List<ReviewResponse> reviews = reviewService.getReviewsByProductId(id);
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id){
        if(rateRepository.existsById(id)){
            rateRepository.deleteById(id);
            return ResponseEntity.ok("Review deleted successfully.");
        }
        return new ResponseEntity<>(new DataError("sản phẩm không tồn tại",404), HttpStatus.NOT_FOUND);
    }
}
