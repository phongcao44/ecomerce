package com.ra.base_spring_boot.services.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ra.base_spring_boot.services.NotificationService;

import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendCartReminder(List<String> tokens) {
        System.out.println("📬 Gửi thông báo đến " + tokens.size() + " thiết bị");

        for (String token : tokens) {
            Notification notification = Notification.builder()
                    .setTitle("🛒 Nhắc nhở giỏ hàng")
                    .setBody("Bạn còn sản phẩm trong giỏ hàng. Đừng bỏ lỡ nhé!")
                    .build();

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .build();

            try {
                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("✅ Gửi thành công: " + response);
            } catch (FirebaseMessagingException e) {
                System.err.println("❌ Lỗi khi gửi tới token " + token + ": " + e.getMessage());
            }
        }
    }
}
