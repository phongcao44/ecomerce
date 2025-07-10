package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderResponse {
    private Long orderId;

    private Long userId;

    private PaymentMethod paymentMethod;

    private LocalDateTime createdAt;

    private AddressResponse  shippingAddress;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private List<OrderItemDetailDTO> orderItems;

}
