package com.ra.base_spring_boot.services;

import java.util.List;

public interface FcmTokenService {
    void saveToken(Long userId, String token, String deviceInfo);
    List<String> getTokensByUserId(Long userId);
}
