package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderRequestSelectedDTO {

    @NotNull(message = "Address ID is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Danh sách cartItemId muốn checkout
    private List<Long> cartItemIds;

    // Dùng nếu có áp dụng voucher
    private Long voucherId;

    // Dùng nếu user muốn dùng điểm
    private Integer usedPoints;
}
