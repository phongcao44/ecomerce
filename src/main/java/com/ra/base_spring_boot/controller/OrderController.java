package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.resp.AddressResponse;
import com.ra.base_spring_boot.dto.resp.OrderResponse;
import com.ra.base_spring_boot.dto.resp.UserResponse;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.repository.IOrderItemRepository;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IOrderService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderRepository iOrderRepository;
    @Autowired
    private IOrderItemRepository iOrderItemRepository;

    @GetMapping("/order/list")
    public ResponseEntity<?> findAll() {
        List<Order> orderEntities = iOrderRepository.findAll();
        if (orderEntities.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy đơn hàng nào", HttpStatus.NOT_FOUND);
        }

        List<OrderResponse> orderResponses = orderEntities.stream().map(order -> {
            User user = order.getUser();
            UserResponse userDto = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();

            Address address = order.getShippingAddress();
            AddressResponse addressResponse = AddressResponse.builder()
                    .id(address.getId())
                    .userId(address.getId())
                    .fulladdress(address.getFullAddress())
                    .phone(address.getPhone())
                    .province(address.getProvince())
                    .recipient_name(address.getRecipientName())
                    .ward(address.getWard())
                    .build();

            return OrderResponse.builder()
                    .orderId(order.getId())
                    .userId(user.getId())
                    .createdAt(order.getCreatedAt())
                    .paymentMethod(order.getPaymentMethod())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .shippingAddress(addressResponse)
                    .build();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(orderResponses);
    }

    @PutMapping("/order/edit/{id}")
    public ResponseEntity<?> edit(@PathVariable Long id, @RequestBody OrderStatus status) {
        try {
            return iOrderRepository.findById(id).<ResponseEntity<?>>map(order -> {
                order.setStatus(status);

                // Nếu trạng thái chuyển sang DELIVERED thì xử lý trừ kho
                if (status == OrderStatus.DELIVERED) {
                    order.getOrderItems().forEach(item -> {
                        ProductVariant variant = item.getVariant();
                        Integer currentStock = variant.getStockQuantity();
                        Integer newStock = currentStock - item.getQuantity();

                        if (newStock < 0) {
                            throw new RuntimeException("Sản phẩm " + variant.getId() + " không đủ tồn kho.");
                        }

                        variant.setStockQuantity(newStock);
                    });
                }

                // Lưu đơn hàng
                Order updatedOrder = orderService.save(order);
                return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
            }).orElseGet(() ->
                    new ResponseEntity<>(new DataError("Không tìm thấy đơn hàng", 404), HttpStatus.NOT_FOUND)
            );
        } catch (Exception e) {
            return new ResponseEntity<>(new DataError("Lỗi xử lý: " + e.getMessage(), 500), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/order/delete/{id}")
    @Transactional
    //Có @Transactional: tất cả sẽ rollback → đảm bảo hoặc tất cả cùng thành công, hoặc tất cả bị hủy
    public ResponseEntity<?> delete(@PathVariable Long id){
        Optional<Order> order = iOrderRepository.findById(id);
        if (order.isEmpty()) {
            return new ResponseEntity<>(new DataError("ko thay uid", 404), HttpStatus.NOT_FOUND);
        }else{
            iOrderItemRepository.deleteOrderItemByOrderId(id);
            iOrderRepository.deleteById(id);
            return new ResponseEntity<>("xóa goy", HttpStatus.OK);
        }
    }

    @GetMapping("/user/order/list")
    public ResponseEntity<?> getMyOrders(@RequestParam(required = false) OrderStatus status,
                                         @AuthenticationPrincipal MyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();

        List<Order> orders;
        if (status != null) {
            orders = orderService.findByUserIdAndStatus(userId, status);
        } else {
            orders = orderService.findByUserId(userId);
        }

        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bạn chưa có đơn hàng nào.");
        }
        return ResponseEntity.ok(orders);
    }


    @GetMapping("/admin/order/rate/cancelled_and_returned")
    public ResponseEntity<Map<String, Double>> getRate() {
        return ResponseEntity.ok(orderService.getCancelAndReturnRate());
    }
}
