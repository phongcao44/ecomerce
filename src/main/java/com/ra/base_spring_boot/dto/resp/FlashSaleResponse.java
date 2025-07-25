package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.DiscountType;
import com.ra.base_spring_boot.model.constants.ProductStatus;
import com.ra.base_spring_boot.model.constants.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FlashSaleResponse {
    private Long id;
    private String flashSaleName;
    private String flashSaleDescription;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UserStatus flashSaleStatus;

    private String productName;
    private String productDescription;
    private BigDecimal price;
    private BigDecimal lowestPrice;
    private BigDecimal discountedPrice;
    private boolean isFlashSale;
    private BigDecimal discountOverrideByFlashSale;
    private String discountType;
    private String brand;
    private ProductStatus productStatus;
    private Double averageRating;
    private Long totalReviews;
    private Integer stockQuantity;
    private Integer variantCount;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private Long returnPolicyId;
    private String returnPolicyTitle;
    private String returnPolicyContent;
    private LocalDateTime createdAt;
    private List<ProductVariantResponseDTO> variants;
}
