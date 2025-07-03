package com.ra.base_spring_boot.dto.req;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AddParentCategoryRequest {
    private String name;

    private String description;
}
