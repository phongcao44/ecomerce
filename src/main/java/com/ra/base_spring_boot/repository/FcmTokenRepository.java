package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserId(Long userId);

    Optional<FcmToken> findByToken(String token);

}
