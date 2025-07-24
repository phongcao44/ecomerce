package com.ra.base_spring_boot.scheduler;

import com.ra.base_spring_boot.model.Cart;
import com.ra.base_spring_boot.services.FcmTokenService;
import com.ra.base_spring_boot.services.ICartService;
import com.ra.base_spring_boot.services.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CartReminderScheduler {

    private final ICartService cartService;
    private final FcmTokenService fcmTokenService;
    private final NotificationService notificationService;

    // Chạy mỗi 10s để test (sản phẩm nên là mỗi ngày)
    @Scheduled(fixedRate = 100000000)
    public void remindUsersAboutCart() {
        log.info("Đang chạy nhắc nhở giỏ hàng...");

        List<Long> userIds = cartService.getUsersWithCartItems();
        for (Long userId : userIds) {
            if (!cartService.hasCartItemsWithProducts(userId)) {
                continue; // Không có sản phẩm => bỏ qua
            }

            List<String> tokens = fcmTokenService.getTokensByUserId(userId);
            if (!tokens.isEmpty()) {
                List<String> sentTokens = notificationService.sendCartReminder(tokens);
                sentTokens.forEach(fcmTokenService::deleteToken); // Xoá token đã gửi
            }
        }


        log.info("Đã gửi xong nhắc nhở giỏ hàng.");
    }
}