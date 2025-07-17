package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestAllDTO {
    @NotNull
    private Long addressId;

    @NotNull
    private PaymentMethod paymentMethod;

    private Long voucherId;

    private Integer usedPoints;
}
