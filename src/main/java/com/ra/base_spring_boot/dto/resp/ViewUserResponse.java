package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.constants.UserRank;
import com.ra.base_spring_boot.model.constants.UserStatus;
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
public class ViewUserResponse {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Set<String> roles;
    private UserRank userRank;
    private UserStatus userStatus;
}
