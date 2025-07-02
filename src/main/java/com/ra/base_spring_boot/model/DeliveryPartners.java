package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.constants.DeliveryPartnersStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "delivery_partners")
public class DeliveryPartners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "contactinfo")
    private String contactInfo;

    @Column(name = "wed")
    private String wed;

    @Enumerated(EnumType.STRING)
    private DeliveryPartnersStatus status;

    private LocalDateTime createdAt;
}
