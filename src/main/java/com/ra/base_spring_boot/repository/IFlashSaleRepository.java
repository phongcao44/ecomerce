package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IFlashSaleRepository extends JpaRepository<FlashSale, Long> {
    List<FlashSaleItem> findFlashSaleItemById(Long id);

    @Query(value = "SELECT f FROM FlashSale f WHERE f.startTime <= :startTime AND f.endTime >= :endTime ORDER BY f.startTime DESC LIMIT 1")
    Optional<FlashSale> findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
