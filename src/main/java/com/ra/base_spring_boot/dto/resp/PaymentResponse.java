package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.PaymentMethod;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PaymentResponse {
    private PaymentMethod paymentMethod;

    private PaymentStatus status;

}
