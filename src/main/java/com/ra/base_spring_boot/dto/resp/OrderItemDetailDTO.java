package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.OrderItem;
import com.ra.base_spring_boot.model.ProductVariant;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderItemDetailDTO {
    private String productName;

    private String color;

    private String size;

    private Integer quantity;

    private BigDecimal priceAtTime;

    public static OrderItemDetailDTO fromOrderItem(OrderItem item) {
        ProductVariant variant = item.getVariant();
        return OrderItemDetailDTO.builder()
                .productName(variant.getProduct().getName())
                .color(variant.getColor().getName())
                .size(variant.getSize().getSizeName())
                .quantity(item.getQuantity())
                .priceAtTime(item.getPriceAtTime())
                .build();
    }

}
