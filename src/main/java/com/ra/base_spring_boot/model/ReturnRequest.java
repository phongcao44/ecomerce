package com.ra.base_spring_boot.model;


import com.ra.base_spring_boot.model.constants.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "return_requests")
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    private Order order;

    private String reason;

    private String mediaUrl; // Đường dẫn ảnh/video

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status; // PENDING, APPROVED, REJECTED
    
    @ManyToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;
}


