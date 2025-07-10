package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.AddUserRequest;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.UserDetailResponse;
import com.ra.base_spring_boot.dto.resp.ViewUserResponse;
import com.ra.base_spring_boot.model.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface IUserService {
    List<ViewUserResponse> findAll();
    void addUser(AddUserRequest addUserRequest);
    User findUser(long UserId) ;
    void save(User user);
    void addRole(long userId, long roleId);
    void changeStatus(Long userId,String status);
    void deleteRole(long userId,long roleId);
    void processOAuthPostLogin(String email, String name);
    User findOrCreate(String email, String name);
    UserDetailResponse findUserDetails(Long userId);
}
