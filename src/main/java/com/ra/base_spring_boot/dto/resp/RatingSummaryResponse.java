package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummaryResponse {
    private Long productId;
    private double averageRating;
    private int totalReviews;
    private Map<Integer, Long> starCountMap; // Optional: đếm số lượng từng mức sao
}
