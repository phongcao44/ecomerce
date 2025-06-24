package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ReviewRequest {
    @Min(1)
    @Max(5)

    private int rating;
    private String comment;
    private Long productId;
}
