package com.ra.base_spring_boot.dto.req;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecificationRequestDTO {

    private Long productId;

    private String specKey;

    private String specValue;
}
