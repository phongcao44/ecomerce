package com.ra.base_spring_boot.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class BannerRequest {
    private String title;
    private String targetUrl;
    private String position;
    private boolean status; // có thể vẫn giữ
    private MultipartFile image;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;

}
