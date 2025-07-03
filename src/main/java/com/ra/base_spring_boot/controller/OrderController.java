package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;

import com.ra.base_spring_boot.dto.req.UpdateOrderStatusRequest;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.*;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
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

    @GetMapping("/admin/order/list")
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

                    .userId(order.getId())

                    .createdAt(order.getCreatedAt())
                    .paymentMethod(order.getPaymentMethod())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .shippingAddress(addressResponse)
                    .build();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(orderResponses);


    }

    @PutMapping("/admin/order/edit/{id}")
    @Transactional
    public ResponseEntity<?> edit(@PathVariable Long id,
                                  @RequestBody UpdateOrderStatusRequest request) {
        try {
            Optional<Order> optionalOrder = iOrderRepository.findById(id);
            if (optionalOrder.isEmpty()) {
                return new ResponseEntity<>(new DataError("Không tìm thấy đơn hàng", 404), HttpStatus.NOT_FOUND);
            }

            Order order = optionalOrder.get();
            OrderStatus status = request.getStatus();
            OrderStatus currentStatus = order.getStatus();

            // Kiểm tra trạng thái hợp lệ
            if (currentStatus == OrderStatus.DELIVERED &&
                    status != OrderStatus.RETURNED &&
                    status != OrderStatus.CANCELLED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new DataError("Đơn hàng đã giao chỉ được đổi sang RETURNED hoặc CANCELLED", 400));
            }

            // Cập nhật trạng thái
            order.setStatus(status);

            // Nếu chuyển sang DELIVERED → trừ kho
            if (status == OrderStatus.DELIVERED) {
                for (var item : order.getOrderItems()) {
                    ProductVariant variant = item.getVariant();
                    int currentStock = variant.getStockQuantity();
                    int newStock = currentStock - item.getQuantity();

                    if (newStock < 0) {
                        throw new RuntimeException("Sản phẩm " + variant.getId() + " không đủ tồn kho.");
                    }

                    variant.setStockQuantity(newStock);
                }
            }

            // Lưu lại đơn hàng
            Order updatedOrder = orderService.save(order);

            // UserResponse
            User user = updatedOrder.getUser();
            UserResponse userDto = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();

            // AddressResponse
            Address address = updatedOrder.getShippingAddress();
            AddressResponse addressResponse = AddressResponse.builder()
                    .id(address.getId())
                    .userId(address.getUser().getId())
                    .fulladdress(address.getFullAddress())
                    .phone(address.getPhone())
                    .province(address.getProvince())
                    .recipient_name(address.getRecipientName())
                    .ward(address.getWard())
                    .build();

            // Danh sách sản phẩm (OrderItemDetailDTO)
            List<OrderItemDetailDTO> orderItemDetailResponses = updatedOrder.getOrderItems().stream()
                    .map(OrderItemDetailDTO::fromOrderItem)
                    .collect(Collectors.toList());

            // Trả về OrderResponse
            OrderResponse response = OrderResponse.builder()
                    .orderId(updatedOrder.getId())
                    .userId(user.getId())
                    .createdAt(updatedOrder.getCreatedAt())
                    .paymentMethod(updatedOrder.getPaymentMethod())
                    .status(updatedOrder.getStatus())
                    .totalAmount(updatedOrder.getTotalAmount())
                    .shippingAddress(addressResponse)
                    .orderItems(orderItemDetailResponses)
                    .build();

            return ResponseEntity.ok(response);

    }

    @PutMapping("/edit/{id}")
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


    @DeleteMapping("/admin/order/delete/{id}")
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

    @GetMapping("/admin/order/detail/{id}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id) {
        Optional<Order> optionalOrder = iOrderRepository.findById(id);
        if (optionalOrder.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng");
        }

        Order order = optionalOrder.get();

        // Customer info
        User user = order.getUser();
        UserResponse customer = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        // Address info
        Address addr = order.getShippingAddress();
        AddressSummary address = AddressSummary.builder()
                .id(addr.getId())
                .fulladdress(addr.getFullAddress())
                .province(addr.getProvince())
                .ward(addr.getWard())
                .recipient_name(addr.getRecipientName())
                .phone(addr.getPhone())
                .build();

        // Order items
        List<OrderItemDetail> itemList = order.getOrderItems().stream().map(item -> {
            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();

            List<ProductImageDTO> imageList = product.getImages().stream()
                    .filter(img -> img.getVariant() == null || img.getVariant().getId().equals(variant.getId()))
                    .map(img -> ProductImageDTO.builder()
                            .id(img.getId())
                            .image_url(img.getImageUrl())
                            .is_main(img.getIsMain())
                            .variant_id(img.getVariant() != null ? img.getVariant().getId() : null)
                            .build())
                    .toList();

            return OrderItemDetail.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .variantId(variant.getId())
                    .color(ColorDTO.builder()
                            .id(variant.getColor().getId())
                            .name(variant.getColor().getName())
                            .hex_code(variant.getColor().getHexCode())
                            .build())
                    .size(SizeDTO.builder()
                            .id(variant.getSize().getId())
                            .name(variant.getSize().getSizeName())
                            .description(variant.getSize().getDescription())
                            .build())
                    .quantity(item.getQuantity())
                    .price(item.getVariant().getPriceOverride())
                    .totalPrice(item.getVariant().getPriceOverride().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .images(imageList)
                    .build();
        }).toList();

        // Voucher
        VoucherSummary voucher = null;
        if (order.getVoucher() != null) {
            voucher = VoucherSummary.builder()
                    .code(order.getVoucher().getCode())
                    .discountAmount(order.getVoucher().getDiscountPercent())
                    .build();
        }

//        // Tổng giá tiền
//        BigDecimal subTotal = itemList.stream()
//                .map(OrderItemDetail::getTotalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
//        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
//        BigDecimal totalAmount = subTotal.subtract(discount).add(shippingFee);
//
        OrderDetailResponse response = OrderDetailResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .paymentStatus(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
               // .fulfillmentStatus(order.getFulfillmentStatus())
                .createdAt(order.getCreatedAt())
              //  .subTotal(subTotal)
              //  .discountAmount(discount)
                //.shippingFee(shippingFee)
             //   .totalAmount(totalAmount)
                .customer(customer)
                .shippingAddress(address)
                .items(itemList)
                .voucher(voucher)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/order/detail/{id}")
    public ResponseEntity<?> getOrderDetailUser(Authentication authentication, @PathVariable Long id) {
        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        Optional<Order> optionalOrder = iOrderRepository.findById(id);
        if (optionalOrder.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng");
        }

        Order order = optionalOrder.get();
        //check var
        if (!order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("hk có quyền nhoa");
        }

        // Customer info
        User user = order.getUser();
        UserResponse customer = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        // Address info
        Address addr = order.getShippingAddress();
        AddressSummary address = AddressSummary.builder()
                .id(addr.getId())
                .fulladdress(addr.getFullAddress())
                .province(addr.getProvince())
                .ward(addr.getWard())
                .recipient_name(addr.getRecipientName())
                .phone(addr.getPhone())
                .build();

        // Order items
        List<OrderItemDetail> itemList = order.getOrderItems().stream().map(item -> {
            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();

            List<ProductImageDTO> imageList = product.getImages().stream()
                    .filter(img -> img.getVariant() == null || img.getVariant().getId().equals(variant.getId()))
                    .map(img -> ProductImageDTO.builder()
                            .id(img.getId())
                            .image_url(img.getImageUrl())
                            .is_main(img.getIsMain())
                            .variant_id(img.getVariant() != null ? img.getVariant().getId() : null)
                            .build())
                    .toList();

            return OrderItemDetail.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .variantId(variant.getId())
                    .color(ColorDTO.builder()
                            .id(variant.getColor().getId())
                            .name(variant.getColor().getName())
                            .hex_code(variant.getColor().getHexCode())
                            .build())
                    .size(SizeDTO.builder()
                            .id(variant.getSize().getId())
                            .name(variant.getSize().getSizeName())
                            .description(variant.getSize().getDescription())
                            .build())
                    .quantity(item.getQuantity())
                    .price(item.getVariant().getPriceOverride())
                    .totalPrice(item.getVariant().getPriceOverride().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .images(imageList)
                    .build();
        }).toList();

        // Voucher
        VoucherSummary voucher = null;
        if (order.getVoucher() != null) {
            voucher = VoucherSummary.builder()
                    .code(order.getVoucher().getCode())
                    .discountAmount(order.getVoucher().getDiscountPercent())
                    .build();
        }

//        // Tổng giá tiền
//        BigDecimal subTotal = itemList.stream()
//                .map(OrderItemDetail::getTotalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
//        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
//        BigDecimal totalAmount = subTotal.subtract(discount).add(shippingFee);
//
        OrderDetailResponse response = OrderDetailResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .paymentStatus(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                // .fulfillmentStatus(order.getFulfillmentStatus())
                .createdAt(order.getCreatedAt())
                //  .subTotal(subTotal)
                //  .discountAmount(discount)
                //.shippingFee(shippingFee)
                //   .totalAmount(totalAmount)
                .customer(customer)
                .shippingAddress(address)
                .items(itemList)
                .voucher(voucher)
                .build();

        return ResponseEntity.ok(response);
    }
}


