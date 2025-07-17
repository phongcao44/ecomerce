package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductSpecificationResponseDTO {

    private Long id;

    private Long productId;

    private String productName;

    private String specKey;

    private String specValue;
}
