package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleVariantDetailResponse {
    private Long flashSaleItemId;
    private Long productId;
    private Long variantId;
    private String productName;
    private String color;
    private String size;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private String discountType;
    private BigDecimal discountedPrice;
    private Integer quantityLimit;
    private Integer soldQuantity;
//    private Double averageRating;
//    private Long totalReviews;
}
