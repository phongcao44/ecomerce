package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.BannerRequest;
import com.ra.base_spring_boot.model.Banner;
import com.ra.base_spring_boot.services.IBannerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/banner")
public class BannerController {
    private final IBannerService iBannerService;
    public BannerController(IBannerService iBannerService) {
        this.iBannerService = iBannerService;
    }

    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners(){
        return ResponseEntity.ok(iBannerService.getAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> createBanner(
BannerRequest request
    ) {
        return ResponseEntity.ok(iBannerService.create(request.getTitle(), request.getPosition(),request.isStatus(), request.getStartAt(),request.getEndAt(),request.getImage()));
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> updateBanner(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String position,
            @RequestParam Boolean status,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(iBannerService.update(id, title, position, status, image));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        iBannerService.delete(id);
        return ResponseEntity.ok().build();
    }

}
