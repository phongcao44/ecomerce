package com.ra.base_spring_boot.dto.req;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CategoryRequest {
    private Long id;

    private String name;

    private String description;

    private Long parentId;
}
