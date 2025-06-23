package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FlashSaleItemRequest {
    private Long flashSaleId;

    private Long variantId;

    private Integer quantity;

    //private Integer soldQuantity;

    private BigDecimal price;

    private DiscountType discountType;
}
