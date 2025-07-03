package com.ra.base_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "distribution_Center")

public class DistributionCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    private DeliveryPartners partner;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "name")
    private String name;


    private String address;

    private Integer province;

    private Integer distributor;

    private String ward;

}
