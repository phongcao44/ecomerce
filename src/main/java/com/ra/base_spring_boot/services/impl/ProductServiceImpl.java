package com.ra.base_spring_boot.services.impl;


import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.constants.ProductStatus;
import com.ra.base_spring_boot.repository.ICategoryRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    @Autowired
    private final IProductRepository productRepository;
    @Autowired
    private final ICategoryRepository categoryRepository;

    @Override
    public List<ProductResponseDTO> findAll() {
        List<Product> products = productRepository.findAll();
        // Convert Entity => DTO
        List<ProductResponseDTO> responseDTOS;
        responseDTOS = products.stream().map(product ->
                ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .status(product.getStatus())
                        .categoryName(product.getCategory().getName())
                        .build()
        ).collect(Collectors.toList());
        return responseDTOS;

    }

    @Override
    public ProductResponseDTO findById(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .status(product.getStatus())
                .brand(product.getBrand())
                .categoryName(product.getCategory().getName())
                .build();

    }

    @Override
    public ProductResponseDTO save(ProductRequestDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .brand(dto.getBrand())
                .status(dto.getStatus())
                .category(category)
                .build();

        product = productRepository.save(product);

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .status(product.getStatus())
                .categoryName(product.getCategory().getName())
                .build();
    }


    @Override
    public ProductResponseDTO changeStatus(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        // Chỉ chuyển đổi giữa Active và InActive
        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.INACTIVE);
        } else if (product.getStatus() == ProductStatus.INACTIVE) {
            product.setStatus(ProductStatus.ACTIVE);
        } else {
            // Không thay đổi nếu là OUT_OF_STOCK hoặc DISCONTINUED
            return null;
        }

        Product updated = productRepository.save(product);

        return ProductResponseDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .brand(updated.getBrand())
                .status(updated.getStatus())
                .categoryName(updated.getCategory().getName())
                .build();

    }


    @Override
    public ProductResponseDTO update(long id, ProductRequestDTO dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setBrand(dto.getBrand());
        existing.setStatus(dto.getStatus());
        existing.setCategory(category);

        Product updated = productRepository.save(existing);

        return ProductResponseDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .brand(updated.getBrand())
                .status(updated.getStatus())
                .categoryName(updated.getCategory().getName())
                .build();
    }


    @Override
    public Page<ProductResponseDTO> pagination(Pageable pageable) {
        List<Product> products = productRepository.findAll(pageable).getContent();
        List<ProductResponseDTO> responseDTOS;
        responseDTOS = products.stream().map(product ->
                ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .status(product.getStatus())
                        .categoryName(product.getCategory().getName())
                        .build()
        ).collect(Collectors.toList());
        return new PageImpl<>(responseDTOS, pageable, responseDTOS.size());

    }

    @Override
    public List<ProductResponseDTO> search(String keyword) {
        List<Product> products = productRepository.findProductByNameContainsIgnoreCase(keyword);
        List<ProductResponseDTO> responseDTOS;
        responseDTOS = products.stream().map(product ->
                ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .status(product.getStatus())
                        .categoryName(product.getCategory().getName())
                        .build()
        ).collect(Collectors.toList());
        return responseDTOS;

    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(id);
    }
}
