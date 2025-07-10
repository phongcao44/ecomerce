package com.ra.base_spring_boot.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ra.base_spring_boot.configuration.VnpayConfig;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.Payment;
import com.ra.base_spring_boot.model.constants.PaymentMethod;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.repository.IPaymentRepository;
import com.ra.base_spring_boot.services.IOrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import static com.ra.base_spring_boot.configuration.VnpayConfig.*;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    @Autowired
    public IOrderRepository orderRepository;
    @Autowired
    public IPaymentRepository paymentRepository;
    @PostMapping("/cod-payment/{orderId}")
    public ResponseEntity<?> createPayment(@PathVariable("orderId") Long orderId){
        Optional<Order> orderOptional = orderRepository.findById(orderId);

        if (orderOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng");
        }

        Order order = orderOptional.get();

        // Kiểm tra phương thức thanh toán có phải là COD
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Phương thức thanh toán không phải là COD");
        }

        // Kiểm tra đã có thanh toán chưa
        if (paymentRepository.existsByOrder(order)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Đơn hàng đã có thanh toán");
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING); // Chờ giao hàng xong
        payment.setPaymentTime(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        return ResponseEntity.ok(savedPayment);
    }

    @GetMapping("/vnpay-payment/{orderId}")
    public ResponseEntity<?> createPayment(@RequestParam("orderId") Long orderId, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //truyền giá vào
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isEmpty()) {
            return new ResponseEntity<>("Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND);
        }
        Order order = orderOptional.get();
        // Kiểm tra phương thức thanh toán có phải là COD
        if (order.getPaymentMethod() != PaymentMethod.CREDIT_CARD &&
                order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER &&
                order.getPaymentMethod() != PaymentMethod.PAYPAL) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ban da chon phuong thuc thanh toan la cod");
        }
        long amount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        System.out.println("Amount : " + amount);
        String bankCode = req.getParameter("bankCode");

        String vnp_TxnRef = VnpayConfig.getRandomNumber(8);
        String vnp_IpAddr = VnpayConfig.getIpAddress(req);

        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
       // vnp_Params.put("vnp_ApiUrl", vnp_ApiUrl);
        vnp_Params.put("vnp_CurrCode", "VND");
        //leen trang thong tin thanh toan
        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_ReturnUrl);

        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + order.getId());
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = req.getParameter("language");
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn");
        }
        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnpayConfig.vnp_PayUrl + "?" + queryUrl;
       // com.google.gson.JsonObject job = new JsonObject();
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", "00");
        responseMap.put("message", "success");
        responseMap.put("data", paymentUrl);
        Gson gson = new Gson();
        //test
        System.out.println("Order ID: " + orderId);
        System.out.println("Amount: " + amount);
        System.out.println("Payment URL: " + paymentUrl);
        System.out.println("HashData: " + hashData);
        System.out.println("SecureHash: " + vnp_SecureHash);

        // resp.getWriter().write(gson.toJson(job));
       // return ResponseEntity.ok(gson.toJson(responseMap));
        return ResponseEntity.ok(responseMap);
    }
    @GetMapping("/payment-info")
    public ResponseEntity<?> getPaymentInfo(@RequestParam(value = "vnp_Amount") String amount,
                                            @RequestParam(value = "vnp_BankCode") String bankCode,
                                            @RequestParam(value = "vnp_OrderInfo") String orderInfo,
                                            @RequestParam(value = "vnp_ResponseCode") String responseCode)
    {
        // Tách orderId từ chuỗi vnp_OrderInfo: "Thanh toan don hang:123"
        String[] parts = orderInfo.split(":");
        if (parts.length < 2) {
            return ResponseEntity.badRequest().body("Invalid vnp_OrderInfo format");
        }

        Long orderId;
        try {
            orderId = Long.parseLong(parts[1].trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid order ID format");
        }

        // Tìm Order
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }

        Order order = orderOptional.get();
       Payment payment = new Payment();
       if (responseCode.equals("00")) {
           payment.setOrder(order);
           payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            payment.setPaymentTime(LocalDateTime.now());
            payment.setStatus(PaymentStatus.COMPLETED);
       }else{
           payment.setOrder(order);
           payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
           payment.setPaymentTime(LocalDateTime.now());
           payment.setStatus(PaymentStatus.FAILED);
          // return ResponseEntity.status(HttpStatus.NOT_FOUND).body("co truc tracj goy");
       }
       Payment savepayment = paymentRepository.save(payment);
      // orderRepository.deleteById(orderId);
       return ResponseEntity.ok(savepayment);
    }
}
