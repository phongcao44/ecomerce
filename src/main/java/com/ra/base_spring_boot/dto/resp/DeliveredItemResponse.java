package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
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
    private OrderStatus orderStatus;
    private Integer quantity;
    private String mediaUrl;
    private BigDecimal price;
    private boolean alreadyRequested;

}
