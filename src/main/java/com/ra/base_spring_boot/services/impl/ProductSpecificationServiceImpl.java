package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ProductSpecificationRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductSpecificationResponseDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductSpecification;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.ProductSpecificationRepository;
import com.ra.base_spring_boot.services.ProductSpecificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    private final ProductSpecificationRepository productSpecificationRepository;

    private final IProductRepository productRepository;

    @Override
    public ProductSpecificationResponseDTO create(ProductSpecificationRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new HttpNotFound("Product not found"));

        ProductSpecification spec = ProductSpecification.builder()
                .product(product)
                .specKey(dto.getSpecKey())
                .specValue(dto.getSpecValue())
                .build();

        productSpecificationRepository.save(spec);

        return ProductSpecificationResponseDTO.builder()
                .id(spec.getId())
                .productId(spec.getProduct().getId())
                .productName(spec.getProduct().getName())
                .specKey(spec.getSpecKey())
                .specValue(spec.getSpecValue())
                .build();
    }

    @Override
    public ProductSpecificationResponseDTO update(Long id, ProductSpecificationRequestDTO dto) {
        ProductSpecification spec = productSpecificationRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("ProductSpecification not found"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new HttpNotFound("Product not found"));

        spec.setProduct(product);
        spec.setSpecKey(dto.getSpecKey());
        spec.setSpecValue(dto.getSpecValue());

        productSpecificationRepository.save(spec);

        return ProductSpecificationResponseDTO.builder()
                .id(spec.getId())
                .productId(spec.getProduct().getId())
                .productName(spec.getProduct().getName())
                .specKey(spec.getSpecKey())
                .specValue(spec.getSpecValue())
                .build();
    }

    @Override
    public List<ProductSpecificationResponseDTO> getAll() {
        return productSpecificationRepository.findAll().stream()
                .map(spec -> ProductSpecificationResponseDTO.builder()
                        .id(spec.getId())
                        .productId(spec.getProduct().getId())
                        .productName(spec.getProduct().getName())
                        .specKey(spec.getSpecKey())
                        .specValue(spec.getSpecValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSpecificationResponseDTO> getByProductId(Long productId) {
        return productSpecificationRepository.findAllByProduct_Id(productId).stream()
                .map(spec -> ProductSpecificationResponseDTO.builder()
                        .id(spec.getId())
                        .productId(spec.getProduct().getId())
                        .productName(spec.getProduct().getName())
                        .specKey(spec.getSpecKey())
                        .specValue(spec.getSpecValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSpecificationResponseDTO> getByProductIdForUser(Long productId) {
        return productSpecificationRepository.findAllByProduct_Id(productId).stream()
                .map(spec -> ProductSpecificationResponseDTO.builder()
                        .id(spec.getId())
                        .productId(spec.getProduct().getId())
                        .productName(spec.getProduct().getName())
                        .specKey(spec.getSpecKey())
                        .specValue(spec.getSpecValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        ProductSpecification spec = productSpecificationRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("ProductSpecification Not Found"));
        productSpecificationRepository.delete(spec);
    }
}
