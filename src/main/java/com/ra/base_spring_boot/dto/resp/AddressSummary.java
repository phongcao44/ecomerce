package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressSummary {
    private Long id;
    private String fulladdress;
    private String province;
    private String district;
    private String ward;
    private String recipient_name;
    private String phone;
}
