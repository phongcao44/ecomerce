package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IFlashSaleRepository extends JpaRepository<FlashSale, Long> {
    List<FlashSaleItem> findFlashSaleItemById(Long id);
}
