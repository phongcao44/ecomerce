package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.AddUserRequest;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.ViewUserResponse;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IRoleService;
import com.ra.base_spring_boot.services.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final IRoleRepository iRoleRepository;
    private final ConversionService conversionService;


    @Override
    public List<ViewUserResponse> findAll() {
        List<User> list = userRepository.findAll();

        return list.stream().map(
         this::convertToResponse).collect(Collectors.toList());
    }

    private ViewUserResponse convertToResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name()) // "ROLE_USER", "ROLE_ADMIN"
                .collect(Collectors.toSet());

        return ViewUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .roles(roleNames)
                .build();
    }

    @Override
    public void addUser(AddUserRequest addUserRequest) {
        Set<Role> roles = new HashSet<>();

        if (addUserRequest.getRoles() == null || addUserRequest.getRoles().isEmpty()) {
            Role role = iRoleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            roles.add(role);
        }

        for(String str : addUserRequest.getRoles()){
            RoleName roleName;
            try {
                roleName = RoleName.valueOf(str.toUpperCase());
            }catch (IllegalArgumentException e){
                throw new RuntimeException("Invalid role: " + str);
            }
            Role role = iRoleRepository.findByName(roleName).orElseThrow(()
                    -> new RuntimeException("Invalid role: " + roleName));
            roles.add(role);
        }

    User user = User.builder()
            .username(addUserRequest.getUsername())
            .password(passwordEncoder.encode(addUserRequest.getPassword()))
            .email(addUserRequest.getEmail())
            .status(UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .roles(roles)
            .build();
    userRepository.save(user);
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public void addRole(long userId, long roleId) {

    }

    @Override
    public void changeStatus(Long userId, String status) {
        User user = userRepository.findById(userId).orElse(null);
        try {
            UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
            user.setStatus(newStatus);
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status value: " + status);
        }

    }

        @Override
    public User findUser(long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
