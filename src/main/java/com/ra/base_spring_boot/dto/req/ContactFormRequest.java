package com.ra.base_spring_boot.dto.req;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ContactFormRequest {
    private String name;
    private String email;
    private String phone;
    private String message;
}
