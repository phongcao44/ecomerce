package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductUserResponse {
    private Long id;


    private String name;

    private String description;

    private BigDecimal price;

    private String brand;




    private String brand;

    private String description;

    private String name;

}
