package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AddressResponse {
    private Long id;


    private Long userId;

    private String fulladdress;

    private String phone;

    private String province;

    private String recipient_name;

    private String ward;



}
