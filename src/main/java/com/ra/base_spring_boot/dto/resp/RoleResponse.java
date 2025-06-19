package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RoleResponse {
    private Long userId;
    private Set<RoleName> roleName;

}
