package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ProductVariantRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;

import java.util.List;

public interface IProductVariantService {
    List<ProductVariantResponseDTO> findAll();

    List<ProductVariantResponseDTO> findByProductId(Long productId);

    ProductVariantResponseDTO create(ProductVariantRequestDTO dto);

    ProductVariantResponseDTO update(Long id, ProductVariantRequestDTO dto);

    void delete(Long id);
}
