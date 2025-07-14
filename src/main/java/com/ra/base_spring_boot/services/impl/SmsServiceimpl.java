package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.services.SmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
@Service
public class SmsServiceimpl implements SmsService {
    @Value("${textbee.apiKey}")
    private String apiKey;

    @Value("${textbee.deviceId}")
    private String deviceId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendSms(String to, String message) {
        String url = "https://api.textbee.dev/api/v1/gateway/devices/" + deviceId + "/send-sms";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        Map<String, Object> body = Map.of(
                "recipients", List.of(to),
                "message", message
        );

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
