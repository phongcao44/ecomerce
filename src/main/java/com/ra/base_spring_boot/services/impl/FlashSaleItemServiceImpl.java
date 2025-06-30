package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.repository.IFlashSaleItemRepository;
import com.ra.base_spring_boot.services.IFlashSaleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleItemServiceImpl implements IFlashSaleItemService {
    @Autowired
    private IFlashSaleItemRepository flashSaleItemRepository;
    @Override
    public FlashSaleItem save(FlashSaleItem flashSaleItem) {
        return flashSaleItemRepository.save(flashSaleItem);
    }
}
