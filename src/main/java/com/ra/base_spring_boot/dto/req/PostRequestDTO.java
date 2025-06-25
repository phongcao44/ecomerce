package com.ra.base_spring_boot.dto.req;


import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Data
public class PostRequestDTO {

    private String title;

    private MultipartFile image;

    private String content;

    private String description;

    private String location;
}
