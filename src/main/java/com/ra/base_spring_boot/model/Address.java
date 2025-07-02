package com.ra.base_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "address")
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "full_address")
    private String fullAddress;

    private String wardCode;
    @Column(name = "ward")
    private String ward;

    private Integer districtId;
    @Column(name = "district")
    private String district;

    private Integer provinceId;
    @Column(name = "province")
    private String province;
}
