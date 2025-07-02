package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderResponse {
    private Long orderId;

    private UserResponse userId;

    private PaymentMethod paymentMethod;

    private LocalDateTime createdAt;

    private AddressResponse shippingAddress;

    private OrderStatus status;

    private BigDecimal totalAmount;

}
