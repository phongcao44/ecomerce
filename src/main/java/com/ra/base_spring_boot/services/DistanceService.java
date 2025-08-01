package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.DistanceInfoResponse;

public interface DistanceService {
    //Double calculateDistanceFromUserToOrderAddress(Long userId, Long orderId);

    DistanceInfoResponse calculateDistanceFromUserToOrderAddress(Long userId, Long orderId);

}
