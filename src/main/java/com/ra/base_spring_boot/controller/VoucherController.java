package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.VoucherRequest;
import com.ra.base_spring_boot.dto.resp.CollectVoucherRequest;
import com.ra.base_spring_boot.dto.resp.VoucherResponse;
import com.ra.base_spring_boot.model.Voucher;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/")
public class VoucherController {


    private final IVoucherService voucherService;
    public VoucherController(IVoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("admin/vouchers")
    public ResponseEntity<VoucherResponse> create(@RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.create(request));
    }

    @PostMapping("user/apply")
    public ResponseEntity<VoucherResponse> applyVoucher(@AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String code) {
        return ResponseEntity.ok(voucherService.applyVoucher(userDetails.getUser().getId(),code));
    }
    @PostMapping("user/collect")
    public ResponseEntity<?> collectVoucher(@RequestBody CollectVoucherRequest request) {
        voucherService.collectVoucher(request.getUserId(), request.getVoucherCode());
        return ResponseEntity.ok("Add voucher to store!");
    }

    @GetMapping("user/available")
    public List<VoucherResponse> getCollectibleVouchers(@AuthenticationPrincipal MyUserDetails userDetails) {
        List<VoucherResponse> vouchers = voucherService.getCollectibleVouchers(userDetails.getUser().getId());
        return vouchers;
    }

    @GetMapping("user/viewVoucher")
    public List<VoucherResponse> getVouchers(@AuthenticationPrincipal MyUserDetails userDetails) {
        List<VoucherResponse> vouchers = voucherService.findVoucherByUserId(userDetails.getUser().getId());
        return vouchers;
    }
}
