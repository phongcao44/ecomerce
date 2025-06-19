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

    private String province;
    private String ward;
    private String district;
    private String phone;
    private String recipientName;
    private String fullAddress;
}
