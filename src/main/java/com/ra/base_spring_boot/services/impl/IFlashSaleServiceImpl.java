package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.repository.IFlashSaleRepository;
import com.ra.base_spring_boot.services.IFlashSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IFlashSaleServiceImpl implements IFlashSaleService {
    @Autowired
    private IFlashSaleRepository flashSaleRepository;
    @Override
    public List<FlashSale> getFlashSale() {
        return flashSaleRepository.findAll();
    }

    @Override
    public FlashSale save(FlashSale flashSale) {
        return flashSaleRepository.save(flashSale);
    }


}
