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
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String image; // Link ảnh hoặc tên file

    @Column(columnDefinition = "TEXT")
    private String content; // Nội dung chính

    @Column(length = 100000000)
    private String description;

    private String location; // Vị trí (Vị trí đăng, vị trí liên quan)

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id") // Liên kết tới người tạo
    private User user;
}

