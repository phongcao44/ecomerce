package com.ra.base_spring_boot.dto.resp;


import com.ra.base_spring_boot.model.constants.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String brand;
    private ProductStatus status;
    private Integer stockQuantity;
    private Integer variantCount;
    private String categoryName;
    private String imageUrl;
    private Long returnPolicyId;
    private String returnPolicyTitle;
    private String returnPolicyContent;
}
