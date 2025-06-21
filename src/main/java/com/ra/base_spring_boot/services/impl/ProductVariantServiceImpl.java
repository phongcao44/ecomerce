package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ProductVariantRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantDetailDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.repository.IColorRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import com.ra.base_spring_boot.repository.ISizeRepository;
import com.ra.base_spring_boot.services.IProductService;
import com.ra.base_spring_boot.services.IProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements IProductVariantService {

    @Autowired
    private IProductVariantRepository productVariantRepository;
    @Autowired
    private IProductRepository productRepository;
    @Autowired
    private IColorRepository colorRepository;
    @Autowired
    private ISizeRepository sizeRepository;



    @Override
    public List<ProductVariantResponseDTO> findAll() {
        List<ProductVariant> variants = productVariantRepository.findAll();

        return variants.stream().map(variant -> ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .productName(variant.getProduct().getName())
                .colorName(variant.getColor().getName())
                .sizeName(variant.getSize().getSizeName())
                .stockQuantity(variant.getStockQuantity())
                .priceOverride(variant.getPriceOverride())
                .build()
        ).collect(Collectors.toList());
    }


    @Override
    public List<ProductVariantResponseDTO> findByProductId(Long productId) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        return variants.stream().map(variant -> ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .productName(variant.getProduct().getName())
                .colorName(variant.getColor().getName())
                .sizeName(variant.getSize().getSizeName())
                .stockQuantity(variant.getStockQuantity())
                .priceOverride(variant.getPriceOverride())
                .build()).collect(Collectors.toList());
    }


    @Override
    public ProductVariantResponseDTO create(ProductVariantRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new HttpNotFound("ProductId Not Found"));
        Color color = colorRepository.findById(dto.getColorId())
                .orElseThrow(() -> new HttpNotFound("ColorId Not Found"));
        Size size = sizeRepository.findById(dto.getSizeId())
                .orElseThrow(() -> new HttpNotFound("SizeId Not Found"));

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .color(color)
                .size(size)
                .stockQuantity(dto.getStockQuantity())
                .priceOverride(dto.getPriceOverride())
                .build();

        variant = productVariantRepository.save(variant);

        return ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .productName(product.getName())
                .colorName(color.getName())
                .sizeName(size.getSizeName())
                .stockQuantity(variant.getStockQuantity())
                .priceOverride(variant.getPriceOverride())
                .build();
    }


    @Override
    public ProductVariantResponseDTO update(Long id, ProductVariantRequestDTO dto) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("ProductVariantId Not Found"));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new HttpNotFound("ProductId Not Found"));
        Color color = colorRepository.findById(dto.getColorId())
                .orElseThrow(() -> new HttpNotFound("ColorId Not Found"));
        Size size = sizeRepository.findById(dto.getSizeId())
                .orElseThrow(() -> new HttpNotFound("SizeId Not Found"));

        variant.setProduct(product);
        variant.setColor(color);
        variant.setSize(size);
        variant.setStockQuantity(dto.getStockQuantity());
        variant.setPriceOverride(dto.getPriceOverride());

        variant = productVariantRepository.save(variant);

        return ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .productName(product.getName())
                .colorName(color.getName())
                .sizeName(size.getSizeName())
                .stockQuantity(variant.getStockQuantity())
                .priceOverride(variant.getPriceOverride())
                .build();
    }

    @Override
    public void delete(Long id) {
        if (!productVariantRepository.existsById(id)) {
            throw new HttpNotFound("ProductVariantId Not Found");
        }
        productVariantRepository.deleteById(id);
    }

    @Override
    public ProductVariantDetailDTO getVariantDetail(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        Product product = variant.getProduct();
        Color color = variant.getColor();
        Size size = variant.getSize();

        return ProductVariantDetailDTO.builder()
                .variantId(variant.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                .brand(product.getBrand())
                .price(product.getPrice())
                .priceOverride(variant.getPriceOverride())
                .stockQuantity(variant.getStockQuantity())
                .colorName(color.getName())
                .colorHex(color.getHexCode())
                .sizeName(size.getSizeName())
                .sizeDescription(size.getDescription())
                .build();
    }
}
