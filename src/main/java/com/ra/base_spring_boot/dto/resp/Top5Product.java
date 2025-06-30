package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Top5Product {
    private Long id;
    private String productName;
    private Double price;
    private Long purchaseCount;
    public static Top5Product from(Product product, Long purchaseCount) {
        return Top5Product.builder()
                .id(product.getId())
                .productName(product.getName())
                .price(product.getPrice().doubleValue())
                .purchaseCount(purchaseCount)
                .build();
    }
}
