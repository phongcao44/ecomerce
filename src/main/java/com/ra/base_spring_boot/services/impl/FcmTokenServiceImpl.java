package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.FcmToken;
import com.ra.base_spring_boot.repository.FcmTokenRepository;
import com.ra.base_spring_boot.services.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenServiceImpl implements FcmTokenService {
    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void saveToken(Long userId, String token, String deviceInfo) {
        Optional<FcmToken> existing = fcmTokenRepository.findByToken(token);
        if (existing.isPresent()) return;

        FcmToken newToken = FcmToken.builder()
                .userId(userId)
                .token(token)
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .build();

        fcmTokenRepository.save(newToken);
    }

    @Override
    public List<String> getTokensByUserId(Long userId) {
        return fcmTokenRepository.findByUserId(userId).stream()
                .map(FcmToken::getToken)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteToken(String token) {
        fcmTokenRepository.deleteByToken(token);
    }
}
