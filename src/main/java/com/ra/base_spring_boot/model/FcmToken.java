package com.ra.base_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"token"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceInfo;

    private LocalDateTime lastActive;

    private LocalDateTime createdAt;

    private Long userId;

    private String token;
}