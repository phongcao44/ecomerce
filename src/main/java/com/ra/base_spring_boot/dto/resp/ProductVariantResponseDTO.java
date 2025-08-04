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

    private String sku;

    private String barcode;

    private String productName;

    private Long colorId;

    private Long sizeId;

    private String colorName;

    private String sizeName;

    private Integer stockQuantity;

    private BigDecimal priceOverride;

    private BigDecimal discountOverrideByFlashSale;

    private String discountType;

    private Integer soldQuantity;

    private BigDecimal finalPriceAfterDiscount;


}
