package com.ra.base_spring_boot.dto.req;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CategoryRequest {
   // private Long id;

    private String name;

    private String description;

    private Long parentId;

    private MultipartFile image;
}
