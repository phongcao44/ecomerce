package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.LocationRequest;
import com.ra.base_spring_boot.model.Location;
import com.ra.base_spring_boot.repository.LocationRepository;
import com.ra.base_spring_boot.services.LocationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LocationServiceImpl implements LocationService {
    @Autowired
    private LocationRepository locationRepository;


    @Override
    @Transactional
    public void saveLocation(Long userId, LocationRequest request) {
        // Kiểm tra nếu user đã có vị trí thì xóa trước
        if (locationRepository.existsByUserId(userId)) {
            locationRepository.deleteByUserId(userId);
        }

        Location location = Location.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        locationRepository.save(location);
    }
}
