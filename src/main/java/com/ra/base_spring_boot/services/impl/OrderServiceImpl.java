package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.services.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


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

}
