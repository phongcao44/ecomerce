package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.PaymentResponse;
import com.ra.base_spring_boot.model.Payment;
import com.ra.base_spring_boot.repository.IPaymentRepository;
import com.ra.base_spring_boot.services.IPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IPaymentServiceImpl implements IPaymentService {
    @Autowired
    private IPaymentRepository paymentRepository;
    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Optional<Payment> optionalPayment = paymentRepository.findByOrderId(orderId);

        return optionalPayment.map(payment -> PaymentResponse.builder()
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .build()
        ).orElse(null);
    }
}
