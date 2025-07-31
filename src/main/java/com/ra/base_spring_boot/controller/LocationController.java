package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.LocationRequest;
import com.ra.base_spring_boot.dto.resp.DistanceInfoResponse;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.DistanceService;
import com.ra.base_spring_boot.services.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    private final DistanceService distanceService;

    @PostMapping("/update")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal MyUserDetails userDetails,
                                            @RequestBody LocationRequest locationRequest) {
        System.out.println("Received location: " + locationRequest);

        if (userDetails == null || userDetails.getUser().getId() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Long userId = userDetails.getUser().getId();
        System.out.println("USER ID from token: " + userId);

        locationService.saveLocation(userId, locationRequest);

        return ResponseEntity.ok("Location updated successfully");
    }

    @GetMapping("/user/{userId}/order/{orderId}")
    public ResponseEntity<?> calculateDistance(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        try {
            DistanceInfoResponse response = distanceService.calculateDistanceFromUserToOrderAddress(userId, orderId);
            return ResponseEntity.ok(Map.of(
                    "distance", response.getDistance(), // đơn vị: mét
                    "userLocation", response.getUserLocation(),
                    "shippingLocation", response.getShippingLocation()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

