package com.ra.base_spring_boot.dto.req;

import lombok.*;

import java.time.LocalDate;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class VoucherRequest {
    private String code;
    private double discountPercent;
    private double maxDiscount;
    private LocalDate startDate;
    private LocalDate endDate;
    private int quantity;
    private double minOrderAmount;
    private long voucherId;
}
