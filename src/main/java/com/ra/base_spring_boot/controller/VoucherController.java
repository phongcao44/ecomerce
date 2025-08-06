package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.CollectVoucherRequest;
import com.ra.base_spring_boot.dto.req.VoucherRequest;
import com.ra.base_spring_boot.dto.resp.VoucherResponse;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IVoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("user/voucher/apply")
    public ResponseEntity<VoucherResponse> applyVoucher(@AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam String code) {
        return ResponseEntity.ok(voucherService.applyVoucher(userDetails.getUser().getId(),code));
    }
    @PostMapping("user/voucher/collect")
    public ResponseEntity<?> collectVoucher(@RequestBody CollectVoucherRequest request) {
        voucherService.collectVoucher(request.getUserId(), request.getVoucherCode());
        return ResponseEntity.ok("Add voucher to store!");
    }

    @GetMapping("user/voucher/available")
    public List<VoucherResponse> getCollectibleVouchers(@AuthenticationPrincipal MyUserDetails userDetails) {
        return voucherService.getCollectibleVouchers(userDetails.getUser().getId());
    }

    @GetMapping("user/voucher/viewVoucher")
    public List<VoucherResponse> getVouchers(@AuthenticationPrincipal MyUserDetails userDetails) {
        return voucherService.findVoucherByUserId(userDetails.getUser().getId());
    }
    @GetMapping("user/voucher/viewVoucherFalse")
    public List<VoucherResponse> getUnusedVouchers(@AuthenticationPrincipal MyUserDetails userDetails) {
        return voucherService.findUnusedVouchersByUserId(userDetails.getUser().getId());
    }
    @PutMapping("admin/voucher/update/{voucherId}")
    public ResponseEntity<VoucherResponse> update(
            @RequestBody VoucherRequest request,
            @PathVariable Long voucherId
    ) {
        return ResponseEntity.ok(voucherService.update(request,voucherId));
    }
    @DeleteMapping("admin/voucher/delete")
    public ResponseEntity<?> delete(@RequestBody VoucherRequest request) {
        voucherService.delete(request.getVoucherId());
        return ResponseEntity.ok("Deleted");
    }
    @GetMapping("admin/voucher/all")
    public ResponseEntity<List<VoucherResponse>> getAllVouchers() {
        List<VoucherResponse> responses = voucherService.getAllVouchers();
        return ResponseEntity.ok(responses);
    }
}
