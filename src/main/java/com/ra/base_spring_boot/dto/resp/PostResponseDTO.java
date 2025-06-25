package com.ra.base_spring_boot.dto.resp;


import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Builder
public class PostResponseDTO {

    private Long id;

    private String title;

    private String image;

    private String content;

    private String description;

    private String location;

    private String authorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
