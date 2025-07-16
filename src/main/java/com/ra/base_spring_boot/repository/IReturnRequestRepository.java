package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.OrderItem;
import com.ra.base_spring_boot.model.ReturnRequest;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByUserId(Long userId);
    boolean existsByOrderAndUser(Order order, User user);
    boolean existsByOrder(Order order);
    boolean existsByOrderItem(OrderItem orderItem);


}
