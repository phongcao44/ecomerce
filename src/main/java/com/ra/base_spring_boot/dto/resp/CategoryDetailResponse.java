package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Category;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CategoryDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;
    private List<CategoryDetailResponse> children;
    private String image;
}
