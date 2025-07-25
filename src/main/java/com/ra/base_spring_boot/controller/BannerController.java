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
@RequestMapping("/api/v1/")
public class BannerController {
    private final IBannerService iBannerService;
    public BannerController(IBannerService iBannerService) {
        this.iBannerService = iBannerService;
    }

    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners(){
        return ResponseEntity.ok(iBannerService.getAll());
    }

    @PostMapping(value = "admin/banners",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> createBanner(BannerRequest request)
    {
        return ResponseEntity.ok(
                iBannerService.create(
                        request.getTitle(),
                        request.getPosition(),
                        request.getTargetUrl(),
                        request.isStatus(),
                        request.getStartAt(),
                        request.getEndAt(),
                        request.getImage()
                )
        );
    }

    @PutMapping(value = "admin/banner/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Banner> updateBanner(
            @PathVariable Integer id,
            BannerRequest request
    ) {
        return ResponseEntity.ok(
                iBannerService.update(
                        id,request.getTitle(),
                        request.getTargetUrl(),
                        request.getPosition(),
                        request.isStatus(),
                        request.getStartAt(),
                        request.getEndAt(),
                        request.getImage()));
    }
    @DeleteMapping("admin/banner/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        iBannerService.delete(id);
        return ResponseEntity.ok().build();
    }

}
