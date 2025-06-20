package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.ProductStatus;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductRequestDTO {
    @NotBlank(message = "Không được để trống")
    private String name;

    @NotBlank(message = "Không được để trống")
    private String description;

    @NotNull(message = "Không được để trống")
    private BigDecimal price;

    @NotBlank(message = "Không được để trống")
    private String brand;

    @NotNull(message = "Không được để trống")
    private ProductStatus status;

    @NotNull(message = "Không được để trống")
    private Long categoryId;
}