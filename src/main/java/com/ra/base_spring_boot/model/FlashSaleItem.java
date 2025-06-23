package com.ra.base_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ra.base_spring_boot.model.constants.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "flash_sales_items")
public class FlashSaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "flash_sale_id")
    private FlashSale flashSale;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;


    @Column(name = "discounted_price")
    private BigDecimal discountedPrice;

    @Column(name = "quantity_limit")
    private Integer quantityLimit;

    @Column(name = "sold_quantity")
    private Integer soldQuantity = 0;
}
