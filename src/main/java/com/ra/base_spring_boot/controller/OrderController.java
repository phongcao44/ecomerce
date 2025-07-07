package com.ra.base_spring_boot.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfWriter;
import com.ra.base_spring_boot.dto.DataError;

import com.ra.base_spring_boot.dto.req.AddressRequest;
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
import com.ra.base_spring_boot.services.ghn.GhnClient;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

import java.awt.Image;
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
    @Autowired
    private GhnClient ghnClient;

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
        }catch (Exception e) {
            return new ResponseEntity<>(new DataError(e.getMessage(), 400), HttpStatus.BAD_REQUEST);
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
                .district(addr.getDistrict())
                .ward(addr.getWardCode())
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

    @GetMapping("/order/pdf/{id}")
    public void exportShippingLabel(@PathVariable Long id, HttpServletResponse response) throws Exception {
        // Lấy dữ liệu đơn hàng từ DB hoặc API (ở đây bạn đã có JSON mẫu)
        OrderDetailResponse order = orderService.getOrderDetail(id); // Tự tạo DTO từ API

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=order_" + id + ".pdf");

        Document document = new Document(PageSize.A6.rotate(), 10, 10, 10, 10); // A6 ngang
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        // 1. Header: Shop Name
        document.add(new Paragraph("Shop: Ecommer", fontBold));

        // 2. Người nhận
        document.add(new Paragraph("Người nhận: " + order.getShippingAddress().getRecipient_name(), fontBold));
        document.add(new Paragraph("SĐT: " + order.getShippingAddress().getPhone(), fontNormal));
        document.add(new Paragraph("Địa chỉ: " + order.getShippingAddress().getFulladdress(), fontNormal));
        document.add(new Paragraph("Phường/Xã: " + order.getShippingAddress().getWard(), fontNormal));
        document.add(new Paragraph("Quận/huyện: " + order.getShippingAddress().getDistrict(), fontNormal));
        document.add(new Paragraph("Tỉnh/Thành: " + order.getShippingAddress().getProvince(), fontNormal));
      //  document.add(new Paragraph("Thành phố: Hồ Chí Minh", fontNormal)); // Nếu cố định

        document.add(Chunk.NEWLINE);

        // 3. Thông tin đơn
        document.add(new Paragraph("mã-đơn:" + id, fontNormal));
        //document.add(new Paragraph("Khối luợng tạm tính: 0.30 KG", fontNormal));
        document.add(new Paragraph("phuong thuc thanh toan: " + order.getPaymentMethod(), fontNormal));

        document.add(Chunk.NEWLINE);

//        // 4. QR Code
//        BarcodeQRCode qr = new BarcodeQRCode("Mã đơn: " + id, 100, 100, null);
//        Image qrImage = qr.getImage();
//        qrImage.scaleAbsolute(60, 60);
//        qrImage.setAlignment(Image.ALIGN_RIGHT);
//        document.add(qrImage);

        document.close();
    }

}


