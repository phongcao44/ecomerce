package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenueResponseDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate time;
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private Integer totalCustomers;
    private Integer totalProducts;
    private Integer totalDelivered;
    private List<ProductResponseDTO> top5BestSellers;
}
