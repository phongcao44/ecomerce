package com.ra.base_spring_boot.dto.resp;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CartItemResponseDTO {
    private Long cartItemId;

    private Long variantId;

    private String productName;

    private String color;

    private String size;

    private Integer quantity;

    private BigDecimal originalPrice;

    private BigDecimal discountedPrice;

    private BigDecimal totalPrice;

    private String discountType;

    private BigDecimal discountAmount;

    private BigDecimal discountOverrideByFlashSale;

    private String imageUrl;
}