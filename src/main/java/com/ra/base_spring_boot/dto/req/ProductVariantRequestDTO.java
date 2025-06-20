package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductVariantRequestDTO {

    @NotNull(message = "Không được để trống")
    private Long productId;

    @NotNull(message = "Không được để trống")
    private Long colorId;

    @NotNull(message = "Không được để trống")
    private Long sizeId;

    @NotNull(message = "Không được để trống")
    private Integer stockQuantity;

    @NotNull(message = "Không được để trống")
    private BigDecimal priceOverride;
}
