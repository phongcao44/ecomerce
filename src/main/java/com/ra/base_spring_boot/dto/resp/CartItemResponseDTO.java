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

    private BigDecimal price;

    private BigDecimal totalPrice;  // tổng tiền = price * quantity

}
