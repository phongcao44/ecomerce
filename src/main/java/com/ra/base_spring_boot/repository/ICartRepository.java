package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Cart;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ICartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(User user);

    List<Cart> findByCreatedAtBefore(LocalDateTime createdAtBefore);

}
