package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetail {
    private Long productId;
    private String productName;
    private Long variantId;
    private ColorDTO color;
    private SizeDTO size;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private List<ProductImageDTO> images;
}
