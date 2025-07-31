package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;

import java.util.List;

public interface IFlashSaleItemService {
    FlashSaleItem save(FlashSaleItem flashSaleItem);
    FlashSaleResponse getTop1();
}
