package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.OrderItem;
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
public class OrderCheckoutResponseDTO {
    private Long orderId;

    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;

    private LocalDateTime createdAt;

    private OrderStatus status;

    private List<OrderItemDetailDTO> items;

    private BigDecimal shippingFee;

    public static OrderCheckoutResponseDTO fromOrder(Order order, List<OrderItem> orderItems) {
        return OrderCheckoutResponseDTO.builder()
                .orderId(order.getId())
                .shippingFee(
                        order.getShippingFee() != null ?
                                BigDecimal.valueOf(order.getShippingFee().getTotal()) :
                                BigDecimal.ZERO
                )
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .status(order.getStatus())
                .items(orderItems.stream()
                        .map(OrderItemDetailDTO::fromOrderItem)
                        .toList())
                .build();
    }

}

