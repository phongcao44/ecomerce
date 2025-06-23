package com.ra.base_spring_boot.dto.req;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CartItemRequestDTO {
    private Long variantId;

    private Integer quantity;
}
