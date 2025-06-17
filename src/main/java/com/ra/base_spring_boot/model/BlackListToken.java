package com.ra.base_spring_boot.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "blacklist_tokens")
public class BlackListToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt; // Giữ để dọn dẹp token sau khi hết hạn

    // Optional: lưu user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
