package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CategoryFlatResponse {
    private Long id;

    private String name;

    private String description;

    private int level;

    private Long parentId;

    private String parentName;

    private String image;
}
