package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.BlackListToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IBlackListRepository extends JpaRepository<BlackListToken, Long> {
    Optional<BlackListToken> findByToken(String token);
    boolean existsByToken(String token);
}
