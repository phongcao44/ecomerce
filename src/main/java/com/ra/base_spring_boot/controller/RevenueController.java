package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.resp.RevenueResponseDTO;
import com.ra.base_spring_boot.services.IOrderService;

import com.ra.base_spring_boot.services.IRevenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/revenue")
public class RevenueController {
    @Autowired
    private IRevenueService revenueService;

    // theo khoảng
    @GetMapping("/range")
    public ResponseEntity<?> getRevenueByRange(@RequestParam String from, @RequestParam String to) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc (định dạng dd-MM-yyyy).");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate fromDate;
        LocalDate toDate;

        try {
            fromDate = LocalDate.parse(from, formatter);
            toDate = LocalDate.parse(to, formatter);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Định dạng ngày không hợp lệ hoặc ngày không tồn tại. Định dạng đúng là dd-MM-yyyy (ví dụ: 30-06-2025)");
        }

        if (fromDate.isAfter(toDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        List<RevenueResponseDTO> list = revenueService.getRevenueByRange(fromDate, toDate);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không có doanh thu nào từ " + fromDate + " đến " + toDate);
        }

        return ResponseEntity.ok(list);
    }

    // theo ngày
    @GetMapping("/day")
    public ResponseEntity<?> getRevenueByDay(@RequestParam int day) {
        if (day < 1 || day > 31) {
            return ResponseEntity.badRequest().body("Ngày không hợp lệ (phải từ 1 đến 31)");
        }

        List<RevenueResponseDTO> result = revenueService.getRevenueByDayOnly(day);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không có doanh thu nào trong các ngày " + day);
        }

        return ResponseEntity.ok(result);
    }


    // theo tháng
    @GetMapping("/month")
    public ResponseEntity<?> getRevenueByMonth(@RequestParam int month, @RequestParam int year) {
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest()
                    .body("Tháng không hợp lệ. Chỉ nhập giá trị từ 1 đến 12");
        } if (year < 2000 || year > 3000) {
            return ResponseEntity.badRequest().body("năm không hợp lệ. phải nhập đầy đủ năm (ví dụ: 2025)");
        }
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<RevenueResponseDTO> list = revenueService.getRevenueByMonth(month, year);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không có doanh thu nào trong tháng " + month + "/" + year);
        }
        return ResponseEntity.ok(list);
    }

    // theo năm
    @GetMapping("/year")
    public ResponseEntity<?> getRevenueByYear(@RequestParam int year) {
        if (year < 2000 || year > 3000) {
            return ResponseEntity.badRequest().body("năm không hợp lệ. phải nhập đầy đủ năm (ví dụ: 2025)");
        }
        List<RevenueResponseDTO> list = revenueService.getRevenueByYear(year);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không có doanh thu nào trong năm " + year);
        }
        return ResponseEntity.ok(list);
    }
}

    // Theo ngày
//    @GetMapping("/day")
//    public ResponseEntity<?> getRevenueByDay(
//            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
//        List<RevenueResponseDTO> list = revenueService.getRevenueByRange(date, date);
//        if (list.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body("Không có doanh thu nào vào ngày: " + date);
//        }
//        return ResponseEntity.ok(list);
//    }

