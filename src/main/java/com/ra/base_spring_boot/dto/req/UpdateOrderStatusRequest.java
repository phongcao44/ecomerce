package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UpdateOrderStatusRequest {
    private OrderStatus status;
}
