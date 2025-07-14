package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.OrderDetailResponse;
import com.ra.base_spring_boot.dto.resp.OrderResponse;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;

import java.util.List;
import java.util.Map;

public interface IOrderService {
    List<Order> findByOrderId(Long orderId);

    Order findById(Long orderId);

    Order save(Order order);


    OrderDetailResponse getOrderDetail(Long id);

    List<OrderResponse> getAllOrderResponses();

    long countByStatus(OrderStatus status);

    Map<String, Double> getCancelAndReturnRate();

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

   // List<Order> getOrderStatusDelivered(Long userId);
}
