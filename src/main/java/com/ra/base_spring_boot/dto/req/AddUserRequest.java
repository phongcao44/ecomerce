package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AddUserRequest {
    private String username;
    private String password;
    private String email;
    private Set<String> roles;
}
