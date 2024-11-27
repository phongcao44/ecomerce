package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FormRegister
{
    @NotBlank(message = "Không được để trống")
    private String fullName;
    @NotBlank(message = "Không được để trống")
    private String username;
    @NotBlank(message = "Không được để trống")
    private String password;
}
