package com.ra.base_spring_boot.dto.resp;


import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Builder
public class ProductImageResponseDTO {
    private Long id;

    private String imageUrl;

    private Boolean isMain;

    private Long productId;

    private String productName;

    private Long variantId;

    private String variantSize;

    private String variantColor;
}
