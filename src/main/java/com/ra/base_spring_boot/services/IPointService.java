package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.UserPointResponse;

import java.util.List;

public interface IPointService {
    UserPointResponse getUserPoints(Long userId);
    List<UserPointResponse> getAllUserRank(Long orderId);

}
