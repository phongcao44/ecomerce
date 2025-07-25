package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.model.FlashSale;

import java.util.List;

public interface IFlashSaleService {
    List<FlashSale> getFlashSale();

    List<FlashSaleResponse> getFlashSaleDetails();

    FlashSale save(FlashSale flashSale);

}
