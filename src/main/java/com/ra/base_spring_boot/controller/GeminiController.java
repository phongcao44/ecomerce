package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.GeminiRequest;
import com.ra.base_spring_boot.dto.resp.ColorDTO;
import com.ra.base_spring_boot.dto.resp.GeminiResponse;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.services.GeminiService;
import com.ra.base_spring_boot.services.IColorService;
import com.ra.base_spring_boot.services.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gemini")
@CrossOrigin(origins = "*")
public class GeminiController {
//    @Autowired
//    private GeminiService geminiService;

    @Autowired
    private IColorService colorService;
    @Autowired
    private IProductService productService;


//    @PostMapping
//    public ResponseEntity<GeminiResponse> chat(@RequestBody GeminiRequest request) {
//        GeminiResponse response = geminiService.generateContent(request);
//        return ResponseEntity.ok(response);
//    }

}
