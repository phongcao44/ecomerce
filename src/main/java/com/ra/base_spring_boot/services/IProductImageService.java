package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ProductImageRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductImageResponseDTO;
import com.ra.base_spring_boot.model.ProductImage;

import java.util.List;

public interface IProductImageService {
    List<ProductImageResponseDTO> findAll();

    ProductImageResponseDTO create(ProductImageRequestDTO dto);

    ProductImageResponseDTO update(Long id, ProductImageRequestDTO dto);

    List<ProductImageResponseDTO> getByProductId(Long productId);

    List<ProductImageResponseDTO> getByVariantId(Long variantId);

    List<ProductImageResponseDTO> findAllImagesByProductName(String productName);

    void delete(Long id);
}

