package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.FlashSaleVariantDetailResponse;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.model.FlashSale;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.util.List;

public interface IFlashSaleService {
    List<FlashSale> getFlashSale();

    //    List<FlashSaleResponse> getFlashSaleDetails();
    List<ProductResponseDTO> getFlashSaleDetails(Long flashSaleId);

    FlashSale save(FlashSale flashSale);

    List<FlashSaleVariantDetailResponse> getFlashSaleItemsByFlashSaleId(Long flashSaleId) throws ChangeSetPersister.NotFoundException;


}
