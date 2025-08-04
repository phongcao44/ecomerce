package com.ra.base_spring_boot.dto.resp;


import com.ra.base_spring_boot.model.constants.ProductStatus;
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
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal lowestPrice;
    private String brand;

    private boolean isFlashSale;
    private BigDecimal discountedPrice;
    private BigDecimal originalPrice;
    private BigDecimal discountOverrideByFlashSale;
    private String discountType;

    private Integer soldQuantity;
    private String slug;
    private Boolean isFavorite;

    private LocalDateTime updatedAt;
    private ProductStatus status;
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

    public List<ProductVariantResponseDTO> variants;

}
