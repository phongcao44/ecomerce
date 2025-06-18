package com.ra.base_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne @JoinColumn(name = "color_id")
    private Color color;

    @ManyToOne @JoinColumn(name = "size_id")
    private Size size;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "price_override")
    private BigDecimal priceOverride;
}
