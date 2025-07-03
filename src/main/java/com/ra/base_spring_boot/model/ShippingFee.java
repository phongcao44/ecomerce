package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.Order;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "shipping_fee")
public class ShippingFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private double total;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
