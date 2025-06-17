package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.services.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService
{
    private final IRoleRepository roleRepository;

    @Override
    public Role findByRoleName(RoleName roleName)
    {
        return roleRepository.findByName(roleName).orElseThrow(() -> new HttpNotFound("role not found"));
    }
}
