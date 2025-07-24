package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data

public class WishListResponse {
    private Long userId;
    private Long wishListId;
    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private String brand;
    private Double averageRating;
    private Long totalReviews;
    private Integer stockQuantity;
    private Integer variantCount;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private LocalDateTime createdAt;
    private List<ProductVariantResponseDTO> variants;
}
