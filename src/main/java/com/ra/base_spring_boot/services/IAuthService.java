package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ChangePasswordRequest;
import com.ra.base_spring_boot.dto.req.ForgotPasswordRequest;
import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.JwtResponse;
import com.ra.base_spring_boot.model.User;

public interface IAuthService
{

    void register(FormRegister formRegister);

    JwtResponse login(FormLogin formLogin);

    void logout(String token, User user);

    void forgotPassword(ForgotPasswordRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    void resetPassword(String token, String newPassword);

}
