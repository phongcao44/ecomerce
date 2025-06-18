package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SearchCategoryRespone {
    private String name;

    private String description;
}
