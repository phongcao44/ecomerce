package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IUserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>
{
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
