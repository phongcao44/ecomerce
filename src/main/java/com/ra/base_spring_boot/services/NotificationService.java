package com.ra.base_spring_boot.services;

import java.util.List;

public interface NotificationService {
    void sendCartReminder(List<String> tokens);
}
