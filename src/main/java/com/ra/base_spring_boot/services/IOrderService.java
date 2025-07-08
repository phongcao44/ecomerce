package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.OrderDetailResponse;
import com.ra.base_spring_boot.dto.resp.OrderResponse;
import com.ra.base_spring_boot.model.Order;

import java.util.List;

public interface IOrderService {
    List<Order> findByOrderId(Long orderId);

    Order findById(Long orderId);

    Order save(Order order);

    OrderDetailResponse getOrderDetail(Long id);

    List<OrderResponse> getAllOrderResponses();
}
