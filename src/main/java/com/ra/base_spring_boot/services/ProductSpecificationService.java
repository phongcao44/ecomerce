package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ProductSpecificationRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductSpecificationResponseDTO;

import java.util.List;

public interface ProductSpecificationService {

    ProductSpecificationResponseDTO create(ProductSpecificationRequestDTO dto);

    ProductSpecificationResponseDTO update(Long id, ProductSpecificationRequestDTO dto);

    List<ProductSpecificationResponseDTO> getAll();

    List<ProductSpecificationResponseDTO> getByProductId(Long productId);

    List<ProductSpecificationResponseDTO> getByProductIdForUser(Long productId);

    void delete(Long id);
}
