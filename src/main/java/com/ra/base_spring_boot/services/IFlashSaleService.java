package com.ra.base_spring_boot.services;

//import com.google.api.gax.paging.Page;
import org.springframework.data.domain.Page;
import com.ra.base_spring_boot.dto.resp.FlashSaleVariantDetailResponse;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.model.FlashSale;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.math.BigDecimal;
import java.util.List;

public interface IFlashSaleService {
    List<FlashSale> getFlashSale();

    //    List<FlashSaleResponse> getFlashSaleDetails();
    List<ProductResponseDTO> getFlashSaleDetails(Long flashSaleId);

    FlashSale save(FlashSale flashSale);

    List<FlashSaleVariantDetailResponse> getFlashSaleItemsByFlashSaleId(Long flashSaleId) throws ChangeSetPersister.NotFoundException;

    Page<ProductResponseDTO> getFlashSaleItemsPaginate(
            Long flashSaleId,
            Long categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String discountRange,
            Integer minRating,
            int page,
            int limit,
            String sortBy,
            String orderBy);
}
