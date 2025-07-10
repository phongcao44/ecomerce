package com.ra.base_spring_boot.dto.resp;


import com.ra.base_spring_boot.model.constants.UserRank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserPointRequest {
    private UserRank userRank;
    private Long userId;
    private Integer userPoints;
    private Integer rankPoints;
}
