package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductViewResponse {
    private Long id;

    private String name;

    private Long viewCount;
}
