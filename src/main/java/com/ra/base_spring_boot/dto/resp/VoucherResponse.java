package com.ra.base_spring_boot.dto.resp;

import lombok.*;

import java.time.LocalDate;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private double discountPercent;
    private double maxDiscount;
    private LocalDate startDate;
    private LocalDate endDate;
    private long quantity;
    private double minOrderAmount;
    private boolean collectible;
    private boolean active;
}
