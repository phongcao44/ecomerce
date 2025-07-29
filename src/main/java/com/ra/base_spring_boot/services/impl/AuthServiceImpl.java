package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ChangePasswordRequest;
import com.ra.base_spring_boot.dto.req.ForgotPasswordRequest;
import com.ra.base_spring_boot.dto.req.FormLogin;
import com.ra.base_spring_boot.dto.req.FormRegister;
import com.ra.base_spring_boot.dto.resp.JwtResponse;
import com.ra.base_spring_boot.dto.resp.OAuth2ResponseDTO;
import com.ra.base_spring_boot.email.EmailService;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.model.constants.TokenStatus;
import com.ra.base_spring_boot.model.constants.UserRank;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
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
    private final IPointService pointService;
    private final IRoleRepository roleRepository;
    private final RestTemplate restTemplate;
    private final IPointRepository pointRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookClientSecret;


    @Override
    public void register(FormRegister formRegister)
    {
        String email = formRegister.getEmail();
        if (userRepository.existsByEmail(formRegister.getEmail())) {
            throw new HttpBadRequest("Email đã tồn tại");
        }
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
        pointService.SetUserPoints(user);
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
    public String getGoogleRedirectUrl(HttpServletRequest request) {
        String redirectUri = "http://localhost:5173/oauth2/redirect"; // Hoặc ní cho vào config .yml

        return UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", "openid", "email", "profile"))
                .queryParam("prompt", "consent")
                .encode()
                .toUriString();
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

        String resetUrl = "http://localhost:5173/reset-password?token=" + token;

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

    @Override
    public Object exchangeGoogleCodeForToken(String code, String redirectUri) {
        try {
            // 1. Decode code nếu có ký tự encode
            String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);

            // 2. Gửi code tới Google để lấy access token
            String tokenUrl = "https://oauth2.googleapis.com/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // THÊM VÀO DÒNG NÀY

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", decodedCode);
            body.add("client_id", googleClientId);
            body.add("client_secret", googleClientSecret);
            body.add("redirect_uri", redirectUri);
            body.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> tokenResponseEntity = restTemplate.postForEntity(tokenUrl, requestEntity, Map.class);
            Map<String, Object> tokenResponse = tokenResponseEntity.getBody();

            if (tokenResponse == null || tokenResponse.get("access_token") == null) {
                throw new RuntimeException("Không lấy được access token từ Google");
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // 3. Dùng access token gọi API lấy thông tin user
            String infoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(infoUrl, HttpMethod.GET, userEntity, Map.class);
            Map<String, Object> userInfo = userInfoResponse.getBody();

            if (userInfo == null || userInfo.get("email") == null) {
                throw new RuntimeException("Không lấy được thông tin người dùng từ Google");
            }

            String email = (String) userInfo.get("email");
            String firstName = (String) userInfo.get("given_name");
            String lastName = (String) userInfo.get("family_name");

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

                String rawPassword = generateRandomPassword(12);
                String encodedPassword = passwordEncoder.encode(rawPassword);

                user = User.builder()
                        .email(email)
                        .username(firstName != null ? firstName : "")
                        .password(encodedPassword)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of(defaultRole))
                        .build();

                userRepository.save(user);

                UserPoint userPoint = UserPoint.builder()
                        .user(user)
                        .rankPoints(0)
                        .totalPoints(0)
                        .userRank(UserRank.valueOf("DONG"))
                        .build();

                pointRepository.save(userPoint);

                String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                StringBuilder emailContent = new StringBuilder();

                emailContent.append("Chào ").append(fullName.trim()).append(",\n\n")
                        .append("🎉 Chào mừng bạn đến với Webshop Ecommerce! 🎉\n\n")
                        .append("Cảm ơn bạn đã lựa chọn đăng nhập bằng Google. Tài khoản của bạn đã được tạo tự động từ thông tin Google để bạn có thể trải nghiệm mua sắm nhanh chóng và tiện lợi.\n\n")
                        .append("🔑 **Thông tin đăng nhập của bạn:**\n")
                        .append("• Tài khoản (Email): ").append(email).append("\n")
                        .append("• Mật khẩu tạm thời: ").append(rawPassword).append("\n\n")
                        .append("Bạn có thể sử dụng tài khoản và mật khẩu trên để đăng nhập theo cách truyền thống.\n")
                        .append("⚠️ **Lưu ý:** Hãy đổi mật khẩu ngay sau lần đăng nhập đầu tiên để đảm bảo an toàn cho tài khoản của bạn.\n\n")
                        .append("📱 Hiện tại, tài khoản của bạn **chưa cập nhật số điện thoại**.\n")
                        .append("👉 Đừng quên thêm số điện thoại trong hồ sơ cá nhân để nhận ưu đãi độc quyền và hỗ trợ nhanh chóng từ đội ngũ chăm sóc khách hàng.\n\n")
                        .append("Chúng tôi rất vui được đồng hành cùng bạn trên hành trình mua sắm sắp tới!\n\n")
                        .append("Trân trọng,\n")
                        .append("— Đội ngũ Website Ecommerce ❤️");

                emailService.sendSimpleMail(
                        email,
                        "Chào mừng đến với Website Ecommerce - Tài khoản của bạn đã sẵn sàng! 🎉",
                        emailContent.toString()
                );

            }

            // 4. Tạo JWT và trả về
            String token = jwtProvider.generateToken(user.getEmail());

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return OAuth2ResponseDTO.builder()
                    .accessToken(token)
                    .roles(roles)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi xử lý Google login: " + e.getMessage(), e);
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }


}


