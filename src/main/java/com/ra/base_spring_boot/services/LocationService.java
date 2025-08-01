package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.LocationRequest;

public interface LocationService {
    void saveLocation(Long userId, LocationRequest request);
}
