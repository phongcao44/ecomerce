package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.VoucherRequest;
import com.ra.base_spring_boot.dto.resp.VoucherResponse;
import com.ra.base_spring_boot.model.User;

import java.util.List;

public interface IVoucherService {
    VoucherResponse findVoucherById(Long id);
    List<VoucherResponse> findVoucherByUserId(Long userId);
    VoucherResponse applyVoucher(Long userId,String code);
    VoucherResponse create(VoucherRequest request);
    void delete(Long id);
    VoucherResponse update(VoucherRequest request);
    void assignWelcomeVoucher(User user);
    void collectVoucher(Long userId, String code);
    List<VoucherResponse> getCollectibleVouchers(Long userId);
    List<VoucherResponse> getAllVouchers();
}
