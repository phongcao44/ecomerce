package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class WishlistItemResponseDTO {
    private Long wishlistId;
    private ProductResponseDTO product;
}
