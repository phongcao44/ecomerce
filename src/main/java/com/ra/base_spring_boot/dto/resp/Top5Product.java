package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductImage;
import com.ra.base_spring_boot.model.Review;
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
    private Double averageRating;
    private Long totalReviews;
    private String images;
    public static Top5Product from(Product product, Long purchaseCount,Double averageRating, Long review) {
        String img =null;
        if(product.getImages()!=null && product.getImages().isEmpty()){
            img = product.getImages().get(0).getImageUrl();
        }

        return Top5Product.builder()
                .id(product.getId())
                .productName(product.getName())
                .price(product.getPrice().doubleValue())
                .purchaseCount(purchaseCount)
                .averageRating(averageRating)
                .totalReviews(review)
                .images(img)
                .build();
    }
}
