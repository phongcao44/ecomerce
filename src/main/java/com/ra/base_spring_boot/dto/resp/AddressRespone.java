package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AddressRespone {
    private Long addressId;
    private String provinceName;
    private String wardName;
    private String districtName;
    private String fullAddress;
    private String phone;
    private String recipientName;
}
