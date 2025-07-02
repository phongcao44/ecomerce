package com.ra.base_spring_boot.dto.req;


import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Data
public class ProductImageRequestDTO {
    private MultipartFile image;
    private Long productId;
    private Long variantId; // optional
    private Boolean isMain;
}
