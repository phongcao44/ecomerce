package com.ra.base_spring_boot.services.impl;


import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.*;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;

import com.ra.base_spring_boot.model.constants.ReturnStatus;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.repository.IReturnRequestRepository;
import com.ra.base_spring_boot.services.IOrderService;
import com.ra.base_spring_boot.services.IPaymentService;
import com.ra.base_spring_boot.services.IProductService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import java.util.List;


@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IPaymentService paymentService;
    @Autowired
    private IReturnRequestRepository returnRequestRepository;
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


    @Override
    public OrderDetailResponse getOrderDetail(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
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
                .district(addr.getDistrict())
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
        return OrderDetailResponse.builder()
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
    }

    @Override
    public List<OrderResponse> getAllOrderResponses() {
        List<Order> orderEntities = orderRepository.findAll();
       return orderEntities.stream().map(order -> {
            User user = order.getUser();
            UserResponse userDto = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
            PaymentResponse paymentResponse = paymentService.getPaymentByOrderId(order.getId());
            return OrderResponse.builder()
                    .orderId(order.getId())
                    .username(userDto.getUsername())
                    .createdAt(order.getCreatedAt())
                    .paymentMethod(order.getPaymentMethod())
                    .payment(paymentResponse)
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .build();
        }).collect(Collectors.toList());
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

   /* public List<DeliveredItemResponse> getOrderStatusDelivered(Long userId) {
        List<Order> ordersDelivered = orderRepository.findByUserIdAndStatus(userId, OrderStatus.DELIVERED);
        List<DeliveredItemResponse> responseList = new ArrayList<>();

        for (Order order : ordersDelivered) {
            for (OrderItem item : order.getOrderItems()) {
                if (returnRequestRepository.existsByOrderItem(item)) {
                    continue; // đã yêu cầu trả hàng → ẩn
                }

                Product product = item.getVariant().getProduct();
                String variantInfo = item.getVariant().getColor().getName() + " - " + item.getVariant().getSize();
                String imageUrl = "";
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    imageUrl = product.getImages().get(0).getImageUrl();

                    responseList.add(new DeliveredItemResponse(
                            item.getId(),
                            order.getId(),
                            product.getName(),
                            item.getQuantity(),
                            imageUrl
                    ));
                }
            }
        }
            return responseList;
    }
*/
   @Override
   public List<DeliveredItemResponse> getDeliveredItemsByUser(User user) {
       List<Order> orders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.DELIVERED);

       return orders.stream()
               .flatMap(order -> order.getOrderItems().stream()
                       .filter(item -> !returnRequestRepository.existsByOrderItem(item)) // lọc item chưa bị trả hàng
                       .map(item -> {
                           Product product = item.getVariant().getProduct();
                           String imageUrl = product.getImages() != null && !product.getImages().isEmpty()
                                   ? product.getImages().get(0).getImageUrl()
                                   : null;
                           Integer quantity = item.getQuantity();
                           BigDecimal price = item.getVariant().getPriceOverride()
                                   .multiply(BigDecimal.valueOf(quantity));
                           return new DeliveredItemResponse(
                                   item.getId(),
                                   order.getId(),
                                   product.getName(),
                                   order.getStatus(),
                                   item.getQuantity(),
                                   imageUrl,
                                   price
                           );
                       })
               ).toList();
   }


    @Override
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }


}

