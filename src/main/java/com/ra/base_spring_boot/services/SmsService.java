package com.ra.base_spring_boot.services;

public interface SmsService {
    void sendSms(String to, String message);
}
