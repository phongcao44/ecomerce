package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.resp.ShippingFeeResponse;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.model.ShippingFee;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.repository.DistributionCenterRepository;
import com.ra.base_spring_boot.repository.IOrderItemRepository;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.repository.ShippingFeeRepository;
import com.ra.base_spring_boot.services.IOrderService;
import com.ra.base_spring_boot.services.ShippingFeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/shipping")
public class DistancePriceController {
    @Autowired
    private ShippingFeeService shippingFeeService;
    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderRepository iOrderRepository;

//    @PostMapping("/calculate/{userId}")
//    public ResponseEntity<?> calculateShippingFee(@PathVariable Long userId) {
////        ShippingFee shippingFee = shippingFeeService.calculateAndSaveShippingFee(userId);
////
////        ShippingFeeResponse response = ShippingFeeResponse.builder()
////                .id(shippingFee.getId())
////                .price(shippingFee.getTotal())
////                .build();
////
////        return ResponseEntity.ok(response);
//    }
    @PutMapping("/edit-by-shipper/{id}")
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
}