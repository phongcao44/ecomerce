package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.AddUserRequest;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.req.UserDetailRequest;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserPoint;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IAddressService;
import com.ra.base_spring_boot.services.IRoleService;
import com.ra.base_spring_boot.services.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final IRoleRepository iRoleRepository;
    private final ConversionService conversionService;
    private final PointServiceImpl pointService;
    @Override
    public List<ViewUserResponse> findAll() {
        List<User> list = userRepository.findAll();

        return list.stream().map(
         this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public Page<ViewUserResponse> getAllUsersPaginateAndFilter(String keyword, String status, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("username")), "%" + keyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("email")), "%" + keyword.toLowerCase() + "%")
                    )
            );
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        return userRepository.findAll(spec, pageable)
                .map(user -> new ViewUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getCreatedAt(),
                        user.getUpdatedAt(),
                        user.getRoles().stream()
                                .map(role -> String.valueOf(role.getName()))
                                .collect(Collectors.toSet()),
                        user.getUserPoint().getUserRank(),
                        user.getStatus()
                ));
    }


    private ViewUserResponse convertToResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name()) // "ROLE_USER", "ROLE_ADMIN"
                .collect(Collectors.toSet());
        UserPoint point = user.getUserPoint();
        //UserPointResponse userPointResponse = pointService.getUserPoints(user.getId());
        return ViewUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .roles(roleNames)
                .userRank(user.getUserPoint().getUserRank())
                .userStatus(user.getStatus())
                .build();
    }

    @Override
    public void addUser(AddUserRequest addUserRequest) {
        Set<Role> roles = new HashSet<>();

        if (addUserRequest.getRoles() == null || addUserRequest.getRoles().isEmpty()) {
            // Gán mặc định ROLE_USER nếu không truyền role
            Role defaultRole = iRoleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            roles.add(defaultRole);
        } else {
            for (String str : addUserRequest.getRoles()) {
                if (str == null || str.trim().isEmpty()) {
                    continue; // Bỏ qua chuỗi rỗng
                }

                RoleName roleName;
                try {
                    roleName = RoleName.valueOf(str.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid role: " + str);
                }

                Role role = iRoleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                roles.add(role);
            }
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
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Role role = iRoleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
        user.getRoles().add(role);
        userRepository.save(user);
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
    public void deleteRole(long userId, long roleId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Role role = iRoleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));

        // Lấy user hiện tại đang thực hiện hành động
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        //Không cho phép tự xóa chính mình
        if (user.getId().equals(currentUser.getId())) {
            throw new RuntimeException("Admin cannot remove their own roles.");
        }

        if (role.getName().equals(RoleName.ROLE_ADMIN)) {
            boolean isTargetUserAdmin = user.getRoles().stream()
                    .anyMatch(r -> r.getName().equals(RoleName.ROLE_ADMIN));

            if (isTargetUserAdmin) {
                throw new RuntimeException("Cannot remove ROLE_ADMIN from an admin user.");
            }
        }
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Override
    public User findUser(long userId) {
        return userRepository.findById(userId).orElse(null);

    }
    public void processOAuthPostLogin(String email, String name) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(name);
            userRepository.save(newUser);
        }
    }
    @Override
    public User findOrCreate(String email, String name) {
        Role defaultRole = iRoleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        System.out.println(">>> Calling findOrCreate with: " + email + ", " + name);

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            System.out.println("User exists: " + email);
            return existingUser.get();
        }

        System.out.println("Creating new user: " + email);
        User user = new User();
        user.setEmail(email);
        user.setUsername(name);
        user.setRoles(Set.of(defaultRole));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        System.out.println("Saved user ID: " + savedUser.getId());

        return userRepository.save(user);
    }

    @Override
    public UserDetailResponse findUserDetails(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Set<RoleResponseDTO> roleResponses = user.getRoles().stream()
                .map(role -> RoleResponseDTO.builder()
                        .id(role.getId())
                        .name(role.getName().name())
                        .description(role.getDescription())
                        .build())
                .collect(Collectors.toSet());

        return UserDetailResponse.builder()
                .userId(user.getId())
                 .userName(user.getUsername())
                .userEmail(user.getEmail())
                .Address(user.getAddresses())
                .status(user.getStatus())
                .role(roleResponses)
                .createTime(user.getCreatedAt())
                .updateTime(user.getUpdatedAt())
                .rank(user.getUserPoint().getUserRank())
                .build();
    }

    @Override
    public UserDetailResponse updateUserDetails(Long userId, UserDetailRequest userDetailRequest) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setUsername(userDetailRequest.getUserName());
        user.setEmail(userDetailRequest.getEmail());
        userRepository.save(user);
        return UserDetailResponse.builder()
                .userId(userId)
                .userName(userDetailRequest.getUserName())
                .userEmail(userDetailRequest.getEmail())
                .build();
    }


}
