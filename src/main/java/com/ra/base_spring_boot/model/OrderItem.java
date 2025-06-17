package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "order_items")
public class OrderItem extends BaseObject {
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price_at_time")
    private BigDecimal priceAtTime;
}
