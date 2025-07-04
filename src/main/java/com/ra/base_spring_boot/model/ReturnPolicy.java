package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.constants.ReturnPolicyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "return_policies")
public class ReturnPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Số ngày được trả hàng: 3 hoặc 7 ngày
    private Integer returnDays;

    // cho phép trả lại mà không có lý do
    private Boolean allowReturnWithoutReason;

    // ACTIVE, INACTIVE
    @Enumerated(EnumType.STRING)
    private ReturnPolicyStatus status;

    // Ai tạo chính sách
    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    // Các sản phẩm đang dùng chính sách này
    @OneToMany(mappedBy = "returnPolicy")
    private List<Product> products;

    // Tự động set thời gian
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}