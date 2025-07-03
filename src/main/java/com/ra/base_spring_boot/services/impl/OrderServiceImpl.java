package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.services.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    private IOrderRepository orderRepository;

    @Override
    public List<Order> findByOrderId(Long orderId) {
        return orderRepository.findAll();
    }

    @Override
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }


    public Map<String, Double> getCancelAndReturnRate() {
        long totalOrders = orderRepository.count();
        long canceled = countByStatus(OrderStatus.CANCELLED);
        long returned = countByStatus(OrderStatus.RETURNED);
        long totalDelivered = orderRepository.countByStatus(OrderStatus.DELIVERED);


        // (Số đơn huỷ / Tổng đơn) × 100%
        double cancelRate = totalOrders == 0 ? 0 : (double) canceled / totalOrders * 100;
        // (Số đơn trả / Tổng đơn giao thành công) × 100%
        double returnRate = totalDelivered == 0 ? 0 : (double) returned / totalDelivered * 100;

        Map<String, Double> result = new HashMap<>();
        result.put("cancelRate", cancelRate);
        result.put("returnRate", returnRate);
        return result;
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

}
