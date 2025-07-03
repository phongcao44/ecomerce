package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.constants.UserRank;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name="user_point")
public class UserPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private Integer totalPoints = 0;     // Dùng để đổi giảm giá
    private Integer rankPoints = 0;      // Dùng để xét hạng
    @Enumerated(EnumType.STRING)
    private UserRank userRank = UserRank.DONG;
}
