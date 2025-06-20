package com.ra.base_spring_boot.dto.resp;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponseDTO {
    private Long id;

    private String productName;

    private String colorName;

    private String sizeName;

    private Integer stockQuantity;

    private BigDecimal priceOverride;
}
