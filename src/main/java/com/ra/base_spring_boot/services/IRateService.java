package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ReviewRequest;
import com.ra.base_spring_boot.dto.resp.RatingSummaryResponse;
import com.ra.base_spring_boot.dto.resp.ReviewResponse;

import java.util.List;

public interface IRateService {
        void createReview(ReviewRequest request, Long userId);
        List<ReviewResponse> getReviewsByProductId(Long productId);
        Double getAverageRatingByProductId(Long productId);
        RatingSummaryResponse getRatingSummaryByProductId(Long productId);

}
