package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.ReturnPolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPolicyResponseDTO {
    private Long id;

    private String title;

    private String content;

    private Integer returnDays;

    private Boolean allowReturnWithoutReason;

    private ReturnPolicyStatus status;

    private String adminName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
