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

    private String recipientName;
    private String phone;
    private String fullAddress;
    private String province;  // Tên tỉnh
    private String district;  // Tên quận
    private String ward;      // Tên phường
}
