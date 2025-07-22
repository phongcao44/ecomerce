package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.GeminiRequest;
import com.ra.base_spring_boot.dto.resp.GeminiResponse;

public interface GeminiService {
    GeminiResponse generateContent(GeminiRequest request);
}
