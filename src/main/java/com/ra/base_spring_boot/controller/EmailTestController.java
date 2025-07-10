package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.services.impl.GmailEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/email")
public class EmailTestController {

    @Autowired
    private GmailEmailService gmailEmailService;
    @GetMapping("/test-gmail")
    public ResponseEntity<String> testEmailGmail() {
        gmailEmailService.sendEmail("phonglx001@gmail.com", "Test Gmail", "<h1>Hello</h1><p>Email bằng SMTP Gmail</p>");
        return ResponseEntity.ok("Gửi email bằng Gmail thành công");
    }

}
