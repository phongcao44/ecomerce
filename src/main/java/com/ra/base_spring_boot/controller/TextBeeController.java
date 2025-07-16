package com.ra.base_spring_boot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class TextBeeController {

    @Value("${textbee.apiKey}")
    private String apiKey;

    @Value("${textbee.deviceId}")
    private String deviceId;

    private final RestTemplate rest = new RestTemplate();

    @PostMapping("/api/send-textbee")
    public ResponseEntity<String> sendSms(@RequestParam String to,
                                          @RequestParam String message) {
        String url = "https://api.textbee.dev/api/v1/gateway/devices/" + deviceId + "/send-sms";
        Map<String, Object> body = Map.of(
                "recipients", List.of(to),
                "message", message
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        try {
            rest.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return ResponseEntity.ok("Đã gửi SMS đến " + to);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }
}

