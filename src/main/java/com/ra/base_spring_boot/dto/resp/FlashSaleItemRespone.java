package com.ra.base_spring_boot.dto.resp;

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
public class FlashSaleItemRespone {
    //ko đổi chương trình km
   // private Long flashSaleId;
//
//    private Long variantId;
//
//    private Long ProductId;
//
//    private Integer quantity;
//
//    private Integer soldQuantity;
//
//    private BigDecimal price;
//
//    private DiscountType discountType;
    private Long id;

    private Long productId;

    //private String productName;

    private Long variantId;

    //private String variantName;

    private BigDecimal discountedPrice;

    private Integer quantityLimit;

    private Integer soldQuantity;

    private DiscountType discountType;
}
