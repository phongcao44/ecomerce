package com.ra.base_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@SQLDelete(sql = "UPDATE Voucher SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private Double discountPercent;

    private Double discountAmount; // Thêm dòng này

    private double maxDiscount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private int quantity;

    private double minOrderAmount;

    private boolean active;

    private int collected;

    private boolean collectible;
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL)
    private Set<UserVoucher> userVouchers;
    private Boolean deleted = false;

}
