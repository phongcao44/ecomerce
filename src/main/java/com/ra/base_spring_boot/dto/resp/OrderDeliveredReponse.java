package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.model.constants.ReturnStatus;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderDeliveredReponse {
    private Long orderId;
    private String productName;
    private String image;
    private ReturnStatus returnStatus;
    private Long productVariantId;
    public static OrderDeliveredReponse from(Long orderId, ProductVariant variant, Product product, ReturnStatus returnStatus) {
        String mainImage = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(null);

        return OrderDeliveredReponse.builder()
                .orderId(orderId)
                .productVariantId(variant.getId())
                .productName(product.getName())
                .image(mainImage)
                .returnStatus(returnStatus)
                .build();
    }
}
