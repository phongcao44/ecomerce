package com.ra.base_spring_boot.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.ra.base_spring_boot.dto.DataError;


import com.ra.base_spring_boot.dto.req.AddressRequest;
import com.ra.base_spring_boot.dto.req.UpdateOrderStatusRequest;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;


import com.ra.base_spring_boot.dto.req.UpdateOrderStatusRequest;

import com.ra.base_spring_boot.dto.resp.AddressResponse;
import com.ra.base_spring_boot.dto.resp.OrderItemDetailDTO;
import com.ra.base_spring_boot.dto.resp.OrderResponse;
import com.ra.base_spring_boot.dto.resp.UserResponse;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import com.ra.base_spring_boot.repository.IOrderItemRepository;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.*;
import com.ra.base_spring_boot.services.ghn.GhnClient;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ra.base_spring_boot.specifications.OrderSpecifications;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.awt.*;

import java.awt.Image;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private IPaymentService paymentService;
    @Autowired
    private IPointService pointService;
    @Autowired
    private SmsService smsService;
    @Autowired
    private IGmailService  gmailService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @GetMapping("/admin/order/paginate")
    public ResponseEntity<?> getAllPaginate(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "desc") String orderBy,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);

        Specification<Order> spec = Specification.where(null);

        if (status != null && !status.isEmpty()) {
            spec = spec.and(OrderSpecifications.hasStatus(status));
        }
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and(OrderSpecifications.hasKeyword(keyword));
        }

        if (userId != null) {
            spec = spec.and(OrderSpecifications.hasUserId(userId));
        }


        Page<Order> orderPage = iOrderRepository.findAll(spec, pageable);

        Page<OrderResponse> responsePage = orderPage.map(order -> {
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
        });

        return ResponseEntity.ok(responsePage);
    }


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
            PaymentResponse paymentResponse = paymentService.getPaymentByOrderId(order.getId());
            return OrderResponse.builder()
                    .orderId(order.getId())
                    .username(userDto.getUsername())
                    .createdAt(order.getCreatedAt())
                    .paymentMethod(order.getPaymentMethod())
                    .payment(paymentResponse)
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
         //   order.setStatus(status);

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
                order.setStatus(status);

                // Gửi email mời đánh giá
                String subject = "Cảm ơn bạn đã mua hàng tại Ecommer!";
                String body = """
            Xin chào %s,

            Cảm ơn bạn đã đặt hàng tại cửa hàng của chúng tôi. 
            Đơn hàng của bạn đã được giao thành công. Chúng tôi rất mong nhận được đánh giá từ bạn.

            Vui lòng nhấn vào link sau để đánh giá sản phẩm:
            https://ecomer/review?orderId=%d

            Trân trọng,
            Đội ngũ Ecommer
            """.formatted(order.getUser().getUsername(), order.getId());

                try {
                    gmailService.sendEmailAfterDelayInMinutes(order.getUser().getEmail(), subject, body, 1);
                } catch (Exception e) {
                    e.printStackTrace(); // Có thể log lỗi nếu cần
                }
            }
            // Gửi SMS (sài khóa lại)
            String phoneNumber = order.getShippingAddress().getPhone();
            String smsMessage = "Cảm ơn bạn đã mua hàng tại Ecommer! Vui lòng đánh giá đơn hàng: https://ecomer/review?orderId=%d".formatted(order.getId());
            scheduler.schedule(() -> {
            try {
                smsService.sendSms(phoneNumber, smsMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
            }, 1, TimeUnit.MINUTES);
            //khóa sms tới đây nè

            // Sau khi mọi thứ hợp lệ thì cập nhật trạng thái
            order.setStatus(status);

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

                    .username(userDto.getUsername())

                    //.userId(user.getId())

                    .createdAt(updatedOrder.getCreatedAt())
                    .paymentMethod(updatedOrder.getPaymentMethod())
                    .status(updatedOrder.getStatus())
                    .totalAmount(updatedOrder.getTotalAmount())

                    .shippingAddress(addressResponse)
                    .orderItems(orderItemDetailResponses)
                    .build();
            if (status == OrderStatus.DELIVERED) {
                pointService.accumulatePoints(updatedOrder);
            }

            return ResponseEntity.ok(response);
        }catch (Exception e) {
            return new ResponseEntity<>(new DataError(e.getMessage(), 400), HttpStatus.BAD_REQUEST);
        }
    }

//    @DeleteMapping("/admin/order/delete/{id}")
//
//                    .shippingAddress(addressResponse)
//                    .orderItems(orderItemDetailResponses)
//                    .build();
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return new ResponseEntity<>(new DataError("Lỗi xử lý: " + e.getMessage(), 500), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//


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
// .filter(img -> img.getVariant() == null || img.getVariant().getId().equals(variant.getId()))
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

//        Document document = new Document(PageSize.A6.rotate(), 10, 10, 20, 20);
//        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
//        document.open();
        Rectangle rectangle = new Rectangle(200, 300); // Rộng 400, cao 250 điểm
        Document document = new Document(rectangle, 10, 10, 10, 10);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        //Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 4);
        //Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        // Sửa font để hỗ trợ Unicode
        BaseFont unicodeFont = BaseFont.createFont("src/main/resources/fonts/times.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font fontNormal = new Font(unicodeFont, 8);
        Font fontBold = new Font(unicodeFont, 10, Font.BOLD);

        // 1. Header: Shop Name
        document.add(new Paragraph("Shop: Ecommer", fontBold));
        //document.add(Chunk.NEWLINE);


        // 2. Người nhận
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(5f);
        infoTable.setSpacingAfter(1f);

        infoTable.addCell(new Phrase("Người nhận", fontNormal));
        infoTable.addCell(new Phrase(order.getShippingAddress().getRecipient_name(), fontNormal));

        infoTable.addCell(new Phrase("SĐT", fontNormal));
        infoTable.addCell(new Phrase(order.getShippingAddress().getPhone(), fontNormal));

        infoTable.addCell(new Phrase("Địa chỉ", fontNormal));
        infoTable.addCell(new Phrase(order.getShippingAddress().getFulladdress(), fontNormal));

        infoTable.addCell(new Phrase("Phường/Xã", fontNormal));
        infoTable.addCell(new Phrase(order.getShippingAddress().getWard(), fontNormal));

        infoTable.addCell(new Phrase("Quận/Huyện", fontNormal));
        infoTable.addCell(new Phrase(order.getShippingAddress().getDistrict(), fontNormal));

        infoTable.addCell(new Phrase("Tỉnh/Thành", fontNormal));
        infoTable.addCell(new Phrase(order.getShippingAddress().getProvince(), fontNormal));

        document.add(infoTable);

       // document.add(Chunk.NEWLINE);

        // 3. Thông tin đơn
        PdfPTable orderTable = new PdfPTable(2);
        orderTable.setWidthPercentage(100);
        orderTable.setSpacingBefore(5f);
        infoTable.setSpacingAfter(1f);

        orderTable.addCell(new Phrase("Mã đơn", fontNormal));
        orderTable.addCell(new Phrase(String.valueOf(id), fontNormal));

        orderTable.addCell(new Phrase("Phương thức thanh toán", fontNormal));
        orderTable.addCell(new Phrase(order.getPaymentMethod().toString(), fontNormal));

        document.add(orderTable);

        document.add(Chunk.NEWLINE);

        // 4. Bảng sản phẩm
        PdfPTable productTable = new PdfPTable(3); // Tên, Số lượng, Giá
        productTable.setWidthPercentage(100);
        productTable.setSpacingBefore(10f);

        productTable.addCell(new PdfPCell(new Phrase("Sản phẩm", fontBold)));
        productTable.addCell(new PdfPCell(new Phrase("Số lượng", fontBold)));
        productTable.addCell(new PdfPCell(new Phrase("Giá", fontBold)));

// Danh sách sản phẩm
        for (OrderItemDetail item : order.getItems()) {
            productTable.addCell(new Phrase(item.getProductName(), fontNormal));
            productTable.addCell(new Phrase(String.valueOf(item.getQuantity()), fontNormal));
            productTable.addCell(new Phrase(item.getPrice().toString(), fontNormal));
        }

        document.add(productTable);

        // Tổng tiền (tùy chọn)
        document.add(Chunk.NEWLINE);
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);

        summaryTable.addCell(new Phrase("Tổng tiền tạm tính", fontNormal));
        summaryTable.addCell(new Phrase(order.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).toString(), fontNormal));

        document.add(summaryTable);

//        // 4. QR Code
//        BarcodeQRCode qr = new BarcodeQRCode("Mã đơn: " + id, 100, 100, null);
//        Image qrImage = qr.getImage();
//        qrImage.scaleAbsolute(60, 60);
//        qrImage.setAlignment(Image.ALIGN_RIGHT);
//        document.add(qrImage);

        document.close();
    }

    @GetMapping("/admin/order/excel")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        List<OrderResponse> orders = orderService.getAllOrderResponses();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=orders.xlsx");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Orders");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Order ID");
        header.createCell(1).setCellValue("Username");
        header.createCell(2).setCellValue("Created At");
        header.createCell(3).setCellValue("Payment Method");
        header.createCell(4).setCellValue("Payment Status");
        header.createCell(5).setCellValue("Status");
        header.createCell(6).setCellValue("Total Amount");

        int rowIdx = 1;
        for (OrderResponse order : orders) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(order.getOrderId());
            row.createCell(1).setCellValue(order.getUsername());
            row.createCell(2).setCellValue(order.getCreatedAt().toString());
            row.createCell(3).setCellValue(order.getPaymentMethod().toString());
            row.createCell(4).setCellValue(order.getPayment() != null ? order.getPayment().getStatus().toString() : "N/A");
            row.createCell(5).setCellValue(order.getStatus().toString());
            row.createCell(6).setCellValue(order.getTotalAmount().doubleValue());
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }


    @GetMapping("/user/order/list")
    public ResponseEntity<?> getMyOrders(@RequestParam(required = false) OrderStatus status,
                                         @AuthenticationPrincipal MyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();

        List<Order> orders = (status != null)
                ? orderService.findByUserIdAndStatus(userId, status)
                : orderService.findByUserId(userId);

        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bạn chưa có đơn hàng nào.");
        }

        List<OrderResponse> responses = orders.stream().map(order -> {
            User user = order.getUser();
            List<Address> userAddress = user.getAddresses();
            System.out.println("orderUser: ========" + userAddress.get(0));
            Address address = order.getShippingAddress();
            System.out.println("ordership : ========= "  + order.getShippingAddress());
            UserResponse userDto = UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();

            AddressResponse addressResponse = AddressResponse.builder()
                    .id(address.getId())
                    .userId(address.getUser().getId())
                    .fulladdress(address.getFullAddress())
                    .phone(address.getPhone())
                    .province(address.getProvince())
                    .recipient_name(address.getRecipientName())
                    .ward(address.getWard())
                    .build();

            return OrderResponse.builder()
                    .orderId(order.getId())
                   // .userId(user.getId())
                    .username(userDto.getUsername())
                    .createdAt(order.getCreatedAt())
                    .paymentMethod(order.getPaymentMethod())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .shippingAddress(addressResponse)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/order/rate/cancelled_and_returned")
    public ResponseEntity<Map<String, Double>> getRate() {
        return ResponseEntity.ok(orderService.getCancelAndReturnRate());
    }
    @GetMapping("/user/orders/delivered")
    public ResponseEntity<List<DeliveredItemResponse>> getDeliveredProducts(
            @AuthenticationPrincipal MyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<DeliveredItemResponse> products = orderService.getDeliveredItemsByUser(userDetails.getUser());
        return ResponseEntity.ok(products);
    }
}


