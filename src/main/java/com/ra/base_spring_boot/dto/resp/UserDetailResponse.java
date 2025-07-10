package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.constants.UserRank;
import com.ra.base_spring_boot.model.constants.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserDetailResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private List<Address> Address;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private UserStatus status;
    private UserRank rank;
    private Set<RoleResponseDTO> role;
}
