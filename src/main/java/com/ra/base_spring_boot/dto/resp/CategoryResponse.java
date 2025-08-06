package com.ra.base_spring_boot.dto.resp;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CategoryResponse {
    private Integer level;

    private Long id;

    private String name;

    private String slug;

    private String description;

    private Long parentId;

    private String image;

}
