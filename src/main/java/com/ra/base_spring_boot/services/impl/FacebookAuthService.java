package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FacebookAuthService {

    private final JwtProvider jwtProvider;
    private final IUserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public String loginWithFacebook(String fbAccessToken) {
        String fbUrl = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + fbAccessToken;

        ResponseEntity<Map> fbResponse = restTemplate.getForEntity(fbUrl, Map.class);
        Map<String, Object> fbData = fbResponse.getBody();

        if (fbData == null || fbData.get("email") == null) {
            throw new RuntimeException("Không lấy được thông tin email từ Facebook");
        }

        String email = (String) fbData.get("email");
        String name = (String) fbData.get("name");

        // Tìm hoặc tạo user trong DB
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(name);
                    return userRepository.save(newUser);
                });

        return jwtProvider.generateTokenFromEmail(user.getEmail());
    }
}
