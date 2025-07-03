package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ChangePasswordRequest;
import com.ra.base_spring_boot.dto.req.ForgotPasswordRequest;
import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.JwtResponse;
import com.ra.base_spring_boot.email.EmailService;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.BlackListToken;
import com.ra.base_spring_boot.model.PasswordResetToken;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.model.constants.TokenStatus;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.IBlackListRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.IPasswordResetTokenRepository;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IAuthService;
import com.ra.base_spring_boot.services.IRoleService;
import com.ra.base_spring_boot.services.IVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService
{
    private final IRoleService roleService;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final IBlackListRepository blackListTokenRepository;
    private final EmailService emailService;
    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final IVoucherService voucherService;


    @Override
    public void register(FormRegister formRegister)
    {
        if (userRepository.existsByEmail(formRegister.getEmail())) {
            throw new HttpBadRequest("Email đã tồn tại");
        }
        String email = formRegister.getEmail();
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            throw new IllegalArgumentException("Email phải kết thúc bằng @gmail.com");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(roleService.findByRoleName(RoleName.ROLE_USER));
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .email(email)
                .username(formRegister.getUsername())
                .password(passwordEncoder.encode(formRegister.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);
        voucherService.assignWelcomeVoucher(user);
    }

    @Override
    public JwtResponse login(FormLogin formLogin)
    {
        Authentication authentication;
        try
        {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(formLogin.getEmail(), formLogin.getPassword()));
        }
        catch (AuthenticationException e)
        {
            throw new HttpBadRequest("Username or password is incorrect");
        }

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        if (userDetails.getUser().getStatus() == UserStatus.INACTIVE)
        {
            throw new HttpBadRequest("Your account is blocked");
        }



        return JwtResponse.builder()
                .accessToken(jwtProvider.generateToken(userDetails.getUsername()))
                .user(userDetails.getUser())
                .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                .build();
    }

    @Override
    public void logout(String token, User user) {
        if (blackListTokenRepository.existsByToken(token)) {
            return;
        }

        BlackListToken blackListToken = BlackListToken.builder()
                .token(token)
                .user(user)
                .expiredAt(LocalDateTime.now().plusMinutes(30)) // Hoặc thời gian thực tế token hết hạn
                .build();

        blackListTokenRepository.save(blackListToken);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // kiểm tra xem tài khoản có tồn tại hay không
        String email = request.getEmail(); // Lấy email từ request DTO

        // kiểm tra xem tài khoản có tồn tại hay không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpBadRequest("Email does not exist"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .status(TokenStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetUrl = "http://localhost:8080/api/v1/auth/reset-password?token=" + token;

        // Gửi email
        emailService.sendResetPasswordEmail(email, resetUrl);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new HttpBadRequest("Invalid token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now()) ||
                resetToken.getStatus() != TokenStatus.ACTIVE) {
            throw new HttpBadRequest("Token expired or already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setStatus(TokenStatus.EXPIRED);
        passwordResetTokenRepository.save(resetToken);
    }


    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpBadRequest("User not found"));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new HttpBadRequest("Old password is incorrect");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }



}


