package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location,Long> {
    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    Location findTopByUserIdOrderByTimestampDesc(Long userId);
}
