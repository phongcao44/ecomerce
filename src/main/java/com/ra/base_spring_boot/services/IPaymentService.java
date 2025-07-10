package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.PaymentResponse;

public interface IPaymentService {
    PaymentResponse getPaymentByOrderId(Long orderId);
}
