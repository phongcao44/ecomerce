package com.ra.base_spring_boot.dto.req;

import lombok.Data;

@Data
public class FcmTokenRequest {
    private String token;
    private String deviceInfo;
}
