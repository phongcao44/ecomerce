package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.Payment;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IPaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findOrderByPaymentMethod(PaymentMethod PaymentMethod);
    //kiểm tra xem thanh toán chưa
    boolean existsByOrder(Order order);

    Optional<Payment> findByOrderId(Long orderId);
}
