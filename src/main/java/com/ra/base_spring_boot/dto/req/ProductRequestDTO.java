package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.ProductStatus;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^[a-z0-9-]*$", message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    private String slug; // Optional, will be generated if not provided

    @NotNull(message = "Không được để trống")
    private ProductStatus status;

    @NotNull(message = "Không được để trống")
    private Long categoryId;

    @NotNull(message = "Không được để trống")
    private Long return_policy_id;
}