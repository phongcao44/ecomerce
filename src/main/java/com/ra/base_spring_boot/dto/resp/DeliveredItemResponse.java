package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveredItemResponse {
    private Long itemId;
    private Long orderId;
    private String productName;
    private Integer quantity;
    private String mediaUrl;
    private BigDecimal price;
}
