package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.Order;

import java.util.List;

public interface IOrderService {
    List<Order> findByOrderId(Long orderId);

    Order findById(Long orderId);

    Order save(Order order);

}
