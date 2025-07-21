package com.ra.base_spring_boot.model;

import jakarta.persistence.Entity;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChatTurn {
    private String role;
    private String message;
}
