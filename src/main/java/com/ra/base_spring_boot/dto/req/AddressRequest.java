package com.ra.base_spring_boot.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AddressRequest {

    private int provinceId;
    private String wardId;
    private int districtId;
    private String phone;
    private String recipientName;
    private String fullAddress;
}
