package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CollectVoucherRequest {
    private Long userId;
    private String voucherCode;
}
