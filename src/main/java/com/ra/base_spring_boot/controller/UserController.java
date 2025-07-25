package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.AddUserRequest;
import com.ra.base_spring_boot.dto.req.UserDetailRequest;
import com.ra.base_spring_boot.dto.req.UserStatusRequest;
import com.ra.base_spring_boot.dto.resp.RoleResponse;
import com.ra.base_spring_boot.dto.resp.UserDetailResponse;
import com.ra.base_spring_boot.dto.resp.ViewUserResponse;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;


import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/")
public class UserController {
    @Autowired
    private IUserService userService;


    @PostMapping("/admin/users/add")
    public ResponseEntity<?> handleAddUser(@RequestBody AddUserRequest addUserRequest) {
        userService.addUser(addUserRequest);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/add")).body(
                ResponseWrapper.builder()
                        .status(HttpStatus.CREATED)
                        .code(201)
                        .data("Add new User")
                        .build()
        );
    }


    @GetMapping("/admin/users")
    public ResponseEntity<List<?>> handleGetAllUsers() {
        List<ViewUserResponse> users = userService.findAll();
        ViewUserResponse  viewUserResponse = new ViewUserResponse(

        );
        return new ResponseEntity<>(users, HttpStatus.OK);
    }


    @GetMapping("/admin/users/paginate")
    public ResponseEntity<?> handleGetAllUsersPaginateAndFilter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String orderBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String rank
    ) {
        Sort.Direction direction = orderBy.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ViewUserResponse> users = userService.getAllUsersPaginateAndFilter(keyword, status, rank, pageable);

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(users)
                        .build()
        );
    }


    @PatchMapping("/admin/users/{userId}/changeRole/{roleId}")
    public ResponseEntity<?> handleChangeRole(@PathVariable long userId, @PathVariable long roleId) {
        userService.addRole(userId, roleId);
        User user = userService.findUser(userId);
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        RoleResponse roleDTO = new RoleResponse(
                userId,
                roles
        );
        return new ResponseEntity<>(roleDTO, HttpStatus.OK);
    }


    @DeleteMapping("/admin/users/{userId}/deleteRole/{roleId}")
    public ResponseEntity<?> handleDeleteRole(@PathVariable long userId, @PathVariable long roleId) {
        userService.deleteRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/admin/users/{userId}/status")
    public ResponseEntity<?> handleStatusChange(@PathVariable long userId, @RequestBody UserStatusRequest status) {
        userService.changeStatus(userId, status.getStatus());
        return ResponseEntity.ok(status);
    }


    @GetMapping("/admin/users/{id}")
    public ResponseEntity<?> getUsersDetail(@PathVariable long id) {
        UserDetailResponse users = userService.findUserDetails(id);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/update")
    public ResponseEntity<?> updateUserDetail(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestBody UserDetailRequest userDetailRequest) {
        UserDetailResponse updateUser = userService.updateUserDetails(userDetails.getUser().getId(), userDetailRequest);
        return ResponseEntity.ok(updateUser);
    }

    @GetMapping("/users/view")
    public ResponseEntity<?> getUserView(@AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(userService.findUserDetails(userDetails.getUser().getId()));
    }

}
