package com.ra.base_spring_boot.dto.req;


import com.ra.base_spring_boot.model.constants.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateReturnStatusRequest {

    private ReturnStatus status;
}
