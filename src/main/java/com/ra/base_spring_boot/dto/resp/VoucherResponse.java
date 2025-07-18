package com.ra.base_spring_boot.dto.resp;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long quantity;
    private double minOrderAmount;
    private boolean collectible;
    private boolean active;
}
