package com.ra.base_spring_boot.dto.req;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class InventoryResponseDTO {
    private Long variantId;

    private String productName;

    private String color;

    private String size;

    private Integer quantity;

    private BigDecimal price;
}
