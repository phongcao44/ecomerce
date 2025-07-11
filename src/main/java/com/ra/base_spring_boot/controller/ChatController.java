package com.ra.base_spring_boot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {
    @GetMapping("/chat")
    public String chat() {
        return "redirect:/chat.html"; // điều hướng đến file HTML tĩnh
    }

}
