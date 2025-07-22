package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.email.EmailService;
import com.ra.base_spring_boot.model.Cart;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.ICartRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.ICartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AbandonedCartService {

    @Autowired
    private ICartRepository cartRepository;

    @Autowired
    private EmailService emailService;

   @Scheduled(cron = "0 0 9 * * *") // Mỗi ngày lúc 9h sáng
    //@Scheduled(fixedRate = 30000L)
    public void notifyAbandonedCarts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Cart> abandonedCarts = cartRepository.findByCreatedAtBefore(thirtyDaysAgo);

        for (Cart cart : abandonedCarts) {
            User user = cart.getUser();
            if (user != null && user.getEmail() != null) {
                emailService.sendCartReminder(user.getEmail(), user.getUsername());
            }
        }
    }
}
