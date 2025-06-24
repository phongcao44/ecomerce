package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;

import java.util.List;

public interface IFlashSaleService {
    List<FlashSale> getFlashSale();

    FlashSale save(FlashSale flashSale);

}
