package com.ra.base_spring_boot.dto.req;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class VoucherRequest {
    private String code;
    private double discountPercent;
    private double maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int quantity;
    private double minOrderAmount;
    private boolean collectible;
    private long voucherId;
}
