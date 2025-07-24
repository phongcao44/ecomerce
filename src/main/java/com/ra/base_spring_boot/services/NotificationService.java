package com.ra.base_spring_boot.services;

import java.util.List;

public interface NotificationService {
    List<String>  sendCartReminder(List<String> tokens);
}
