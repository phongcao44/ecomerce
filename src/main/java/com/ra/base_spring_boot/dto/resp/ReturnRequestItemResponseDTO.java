package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ra.base_spring_boot.model.constants.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestItemResponseDTO {
    private Long id;

    private Long orderId;

    private String reason;

    private String mediaUrl;

    private ReturnStatus status;

    private LocalDateTime createdAt;

    private String fullName;

    private String productName;

    private Integer quantity;

    private BigDecimal price;
}
