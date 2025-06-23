package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductVariantDetailDTO {
    private Long variantId;
    private String productName;
    private String productDescription;
    private String brand;
    private BigDecimal price;             // từ product
    private BigDecimal priceOverride;     // nếu có
    private int stockQuantity;

    private String colorName;
    private String colorHex;

    private String sizeName;
    private String sizeDescription;
}

