package com.ra.base_spring_boot.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ReturnRequestDTO {

    private Long orderId;
    private Long itemId;
    private String reason;
    private MultipartFile media; //  file ảnh hoặc video
    // link ảnh/video (đã upload Cloudinary...)
}
