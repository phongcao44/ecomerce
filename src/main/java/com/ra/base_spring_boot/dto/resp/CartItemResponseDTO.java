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

    private String productName;

    private String color;

    private String size;

    private Integer quantity;

    private BigDecimal originalPrice;         // Giá gốc

    private BigDecimal discountedPrice;       // Giá sau giảm

    private BigDecimal totalPrice;            // discountedPrice * quantity

    private String discountType;              // PERCENTAGE / AMOUNT

    private BigDecimal discountAmount;        // Giá giảm được bao nhiêu

    private BigDecimal discountOverrideByFlashSale;
}