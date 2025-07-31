package com.ra.base_spring_boot.dto.resp;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class OrderDetailResponse {
    private Long orderId;
    private String orderCode;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private String fulfillmentStatus;
    private LocalDateTime createdAt;
    private String note; // Thêm
    private String cancellationReason; // Thêm
    private LocalDateTime cancelledAt; // Thêm
    private LocalDateTime updatedAt;

    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;

    private UserResponse customer;
    private AddressSummary shippingAddress;
    private List<OrderItemDetail> items;
    private VoucherSummary voucher;

}