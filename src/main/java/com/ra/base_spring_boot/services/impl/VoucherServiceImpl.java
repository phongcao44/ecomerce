package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.VoucherRequest;
import com.ra.base_spring_boot.dto.resp.VoucherResponse;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserVoucher;
import com.ra.base_spring_boot.model.Voucher;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.IUserVoucherRepository;
import com.ra.base_spring_boot.repository.IVoucherRepository;
import com.ra.base_spring_boot.services.ICartService;
import com.ra.base_spring_boot.services.IVoucherService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoucherServiceImpl implements IVoucherService {
    private final IVoucherRepository iVoucherRepository;
    private final IUserRepository iUserRepository;
    private final IUserVoucherRepository iUserVoucherRepository;
    private final ICartService cartService;
    public VoucherServiceImpl(
            IVoucherRepository iVoucherRepository,
            IUserRepository iUserRepository,
            IUserVoucherRepository iUserVoucherRepository,
            ICartService cartService)
    {
        this.iVoucherRepository = iVoucherRepository;
        this.iUserRepository = iUserRepository;
        this.iUserVoucherRepository = iUserVoucherRepository;
        this.cartService = cartService;
    }

    @Override
    public VoucherResponse findVoucherById(Long id) {
        return null;
    }

    @Override
    public List<VoucherResponse> findVoucherByUserId(Long userId) {
        User user = iUserRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));
        List<UserVoucher> userVouchers = iUserVoucherRepository.findAllByUser(user);
        return userVouchers.stream()
                .map(uv -> {
                    Voucher v = uv.getVoucher();
                    return VoucherResponse.builder()
                            .id(v.getId())
                            .code(v.getCode())
                            .discountPercent(v.getDiscountPercent())
                            .maxDiscount(v.getMaxDiscount())
                            .startDate(v.getStartDate())
                            .endDate(v.getEndDate())
                            .minOrderAmount(v.getMinOrderAmount())
                            .collectible(v.isCollectible())
                            .active(v.isActive())
                            .build();

    })
                .collect(Collectors.toList());
    }

    @Override
    public VoucherResponse applyVoucher(Long userID, String code) {
        Voucher voucher = iVoucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher not exist!"));

        User user = iUserRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        UserVoucher userVoucher = iUserVoucherRepository.findByUserAndVoucher(user, voucher)
                .orElseThrow(() -> new RuntimeException("Voucher not exist!"));

        if (userVoucher.isUsed()) {
            throw new RuntimeException("Voucher is used!");
        }

        BigDecimal orderAmount = cartService.getCartTotal(userID);

        LocalDateTime now = LocalDateTime.now();
        if (!voucher.isActive() || voucher.getQuantity() <= 0
                || now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            throw new RuntimeException("Voucher expired");
        }

        if (orderAmount.compareTo(BigDecimal.valueOf(voucher.getMinOrderAmount())) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
        }

        userVoucher.setUsed(true);
        userVoucher.setUsedAt(LocalDateTime.now());
        iUserVoucherRepository.save(userVoucher);

        VoucherResponse response = mapToResponse(voucher);
        return response;
    }





    @Override
    public VoucherResponse create(VoucherRequest request) {
        Voucher voucher = Voucher.builder()
                .code(request.getCode())
                .discountPercent(request.getDiscountPercent())
                .discountAmount(request.getDiscountAmount())
                .maxDiscount(request.getMaxDiscount())
                .minOrderAmount(request.getMinOrderAmount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .quantity(request.getQuantity())
                .active(request.isActive())
                .collectible(request.isCollectible())
                .build();
        iVoucherRepository.save(voucher);
        VoucherResponse response = mapToResponse(voucher);
    return response;
    }

    @Override
    public void delete(Long id) {
        Voucher voucher = iVoucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not exist!"));
        iVoucherRepository.delete(voucher);
    }

    @Override
    public VoucherResponse update(VoucherRequest voucherRequest, Long voucherId) {
        System.out.println(voucherRequest.getVoucherId() + "day chinh la voucher ID");
        Voucher voucher = iVoucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not exist!"));
        voucher.setCode(voucherRequest.getCode());
        voucher.setDiscountPercent(voucherRequest.getDiscountPercent());
        voucher.setMaxDiscount(voucherRequest.getMaxDiscount());
        voucher.setDiscountAmount(voucherRequest.getDiscountAmount());
        voucher.setMinOrderAmount(voucherRequest.getMinOrderAmount());
        voucher.setStartDate(voucherRequest.getStartDate());
        voucher.setEndDate(voucherRequest.getEndDate());
        voucher.setQuantity(voucherRequest.getQuantity());
        voucher.setCollectible(voucherRequest.isCollectible());
        voucher.setActive(voucherRequest.isActive());
        iVoucherRepository.save(voucher);

        return VoucherResponse.builder()
                .id(voucherId)
                .code(voucherRequest.getCode())
                .discountPercent(voucherRequest.getDiscountPercent())
                .maxDiscount(voucherRequest.getMaxDiscount())
                .discountAmount(voucherRequest.getDiscountAmount())
                .minOrderAmount(voucherRequest.getMinOrderAmount())
                .startDate(voucherRequest.getStartDate())
                .endDate(voucherRequest.getEndDate())
                .quantity(voucherRequest.getQuantity())
                .collectible(voucherRequest.isCollectible())
                .active(voucherRequest.isActive())
                .build();
    }

    private VoucherResponse mapToResponse(Voucher v) {
        return new VoucherResponse(
                v.getId(), v.getCode(), v.getDiscountPercent(), v.getDiscountAmount(),
                v.getMaxDiscount(), v.getStartDate(), v.getEndDate(),
                v.getQuantity(), v.getMinOrderAmount(),v.isCollectible(), v.isActive()
        );
    }

    @Override
    public void assignWelcomeVoucher(User user) {
        Voucher voucher = iVoucherRepository.findByCode("WELCOME")
                .orElseThrow(() -> new HttpBadRequest("Voucher not exist!"));
        UserVoucher userVoucher = new UserVoucher();
        userVoucher.setUser(user);
        userVoucher.setVoucher(voucher);
        iUserVoucherRepository.save(userVoucher);
    }

    @Override
    public void collectVoucher(Long userId, String code) {
        User user = iUserRepository.findById(userId)
                .orElseThrow(() -> new HttpBadRequest("Voucher not exist!"));
        Voucher voucher = iVoucherRepository.findByCode(code)
                .orElseThrow(() -> new HttpBadRequest("Voucher not exist!"));
        if(!voucher.isCollectible()) {
            throw new HttpBadRequest("This voucher cannot be collected");
        }
        if(iUserVoucherRepository.existsByUserAndVoucher(user,voucher)) {
            throw new HttpBadRequest("This voucher have been collected");
        }
        if(voucher.getCollected()>= voucher.getQuantity()){
            throw new HttpBadRequest("This voucher have been out of stock");
        }
        UserVoucher userVoucher = iUserVoucherRepository
                .findByUserAndVoucher(user, voucher)
                .orElse(null);

        if(iUserVoucherRepository.existsByUserAndVoucher(user,voucher)) {
            throw new HttpBadRequest("You have already collected this voucher");
        }else{
            userVoucher = new UserVoucher();
            userVoucher.setUser(user);
            userVoucher.setVoucher(voucher);
            userVoucher.setUsed(false);
        }
        iUserVoucherRepository.save(userVoucher);
        voucher.setCollected(voucher.getCollected() + 1);
        iVoucherRepository.save(voucher);
    }

    @Override
    public List<VoucherResponse> getCollectibleVouchers(Long userId) {
        List<Voucher> vouchers = iVoucherRepository.findCollectibleVouchersAvailable();
        return vouchers.stream()
                .filter(v -> !iUserVoucherRepository.existsByUserIdAndVoucherId(userId, v.getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    private VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountPercent(voucher.getDiscountPercent())
                .maxDiscount(voucher.getMaxDiscount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .quantity(voucher.getQuantity())
                .minOrderAmount(voucher.getMinOrderAmount())
                .collectible(voucher.isCollectible())
                .build();
    }
    @Override
    public List<VoucherResponse> getAllVouchers(){
        List<Voucher> vouchers = iVoucherRepository.findAll();
        return  vouchers.stream().map(
                voucher -> VoucherResponse.builder()
                        .id(voucher.getId())
                        .code(voucher.getCode())
                        .discountPercent(voucher.getDiscountPercent() != null ? voucher.getDiscountPercent() : 0)
                        .discountAmount(voucher.getDiscountAmount() != null ? voucher.getDiscountAmount() : 0)
                        .maxDiscount(voucher.getMaxDiscount())
                        .startDate(voucher.getStartDate())
                        .endDate(voucher.getEndDate())
                        .quantity(voucher.getQuantity())
                        .minOrderAmount(voucher.getMinOrderAmount())
                        .collectible(voucher.isCollectible())
                        .active(voucher.isActive())
                        .build()
        ).collect(Collectors.toList());

}}
