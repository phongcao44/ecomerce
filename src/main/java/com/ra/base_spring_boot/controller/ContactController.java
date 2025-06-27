package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.ContactFormRequest;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IBannerService;
import com.ra.base_spring_boot.services.IContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {
    private final IContactService iContactService;
    public ContactController(IContactService iContactService) {
        this.iContactService = iContactService;
    }
    @PostMapping
    public ResponseEntity<?> sendContactForm(@AuthenticationPrincipal MyUserDetails user, @RequestBody ContactFormRequest request) {
        System.out.println("user = " + user);
        System.out.println("user.getUser() = " + (user != null ? user.getUser() : null));

        Long userId = (user != null && user.getUser() != null) ? user.getUser().getId() : null;
        iContactService.create(userId, request);
        return ResponseEntity.ok("Sent successfully");
    }

}

