package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.ReturnPolicyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ReturnPolicyRequestDTO {

    @NotNull(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Nội dung không được để trống")
    private String content;

    @NotNull(message = "Số ngày đổi trả không được để trống")
    private Integer returnDays;

    private Boolean allowReturnWithoutReason;

    @NotNull(message = "Trạng thái không được để trống")
    private ReturnPolicyStatus status;
}

