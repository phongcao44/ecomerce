package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.FcmTokenRequest;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/fcm-token")
@RequiredArgsConstructor
public class FcmTokenController {
    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<?> saveToken(@RequestBody FcmTokenRequest request) {
        System.out.println("➡️ TOKEN: " + request.getToken());
        System.out.println("➡️ DEVICE: " + request.getDeviceInfo());


        fcmTokenService.saveToken(1L, request.getToken(), request.getDeviceInfo());
        return ResponseEntity.ok("Token saved successfully");
    }
}
