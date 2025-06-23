package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FlashSaleRequest {
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UserStatus status;
}
