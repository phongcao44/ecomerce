package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.OrderItem;
import com.ra.base_spring_boot.model.ProductImage;
import com.ra.base_spring_boot.model.ProductVariant;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderItemDetailDTO {
    private Long variantId;

    private String productName;

    private String color;

    private String size;

    private String image;

    private Integer quantity;

    private BigDecimal priceAtTime;


    public static OrderItemDetailDTO fromOrderItem(OrderItem item) {
        ProductVariant variant = item.getVariant();

        String imageUrl = variant.getProduct().getImages().stream()
                .filter(ProductImage::getIsMain)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);

        return OrderItemDetailDTO.builder()
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .color(variant.getColor() != null ? variant.getColor().getName() : null)
                .size(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                .image(imageUrl) // Hoặc getImage() tùy theo entity bạn định nghĩa
                .quantity(item.getQuantity())
                .priceAtTime(item.getPriceAtTime())
                .build();
    }
}