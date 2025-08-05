package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.constants.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "products")
@Where(clause = "deleted = false")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "brand")
    private String brand;

    @Column(name = "slug", unique = true)
    private String slug;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product")
    private List<ProductVariant> variants;

    @OneToMany(mappedBy = "product")
    private List<ProductImage> images;

    @ManyToOne
    @JoinColumn(name = "return_policy_id")
    private ReturnPolicy returnPolicy;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @PreUpdate
    public void validateUpdatedAt() {
        if (updatedAt != null && createdAt != null && !updatedAt.isAfter(createdAt)) {
            throw new IllegalStateException("updatedAt must be after createdAt");
        }
    }
}