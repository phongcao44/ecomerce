package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ContactResponse {
    private Long id;
    private String name;
    private String comment;
    private String phone;
    private String email;
}
