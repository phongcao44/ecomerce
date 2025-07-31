package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateStockRequestDTO {
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be >= 0")
    private Integer stockQuantity;
}
