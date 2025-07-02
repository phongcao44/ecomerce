package com.ra.base_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private double discountPercent;
    private double maxDiscount;

    private LocalDate startDate;
    private LocalDate endDate;

    private int quantity;

    private double minOrderAmount;

    private boolean active;

    private int collected;

    private boolean collectible;
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL)
    private Set<UserVoucher> userVouchers;

}
