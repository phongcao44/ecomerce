package com.ra.base_spring_boot.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ra.base_spring_boot.dto.req.ProductImageRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductImageResponseDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductImage;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.repository.IProductImageRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import com.ra.base_spring_boot.services.IProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements IProductImageService {

    private final IProductRepository productRepository;

    private final IProductVariantRepository variantRepository;

    private final IProductImageRepository productImageRepository;

    private final Cloudinary cloudinary;

    private String uploadImage(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "product_images"));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Upload image failed", e);
        }
    }


    @Override
    public List<ProductImageResponseDTO> findAll() {
        return productImageRepository.findAll()
                .stream()
                .map(image -> ProductImageResponseDTO.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .isMain(image.getIsMain())
                        .productId(image.getProduct().getId())
                        .productName(image.getProduct().getName())
                        .variantId(image.getVariant() != null ? image.getVariant().getId() : null)
                        .variantSize(image.getVariant() != null && image.getVariant().getSize() != null
                                ? image.getVariant().getSize().getSizeName() : null)
                        .variantColor(image.getVariant() != null && image.getVariant().getColor() != null
                                ? image.getVariant().getColor().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public ProductImageResponseDTO create(ProductImageRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant variant = null;
        if (dto.getVariantId() != null && dto.getVariantId() > 0) {
            variant = variantRepository.findById(dto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));
        }

        String imageUrl = uploadImage(dto.getImage());

        ProductImage image = ProductImage.builder()
                .product(product)
                .variant(variant)
                .imageUrl(imageUrl)
                .isMain(dto.getIsMain() != null && dto.getIsMain())
                .build();

        ProductImage saved = productImageRepository.save(image);

        return ProductImageResponseDTO.builder()
                .id(saved.getId())
                .imageUrl(saved.getImageUrl())
                .isMain(saved.getIsMain())
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getName())
                .variantId(saved.getVariant() != null ? saved.getVariant().getId() : null)
                .variantSize(saved.getVariant() != null && saved.getVariant().getSize() != null
                        ? saved.getVariant().getSize().getSizeName() : null)
                .variantColor(saved.getVariant() != null && saved.getVariant().getColor() != null
                        ? saved.getVariant().getColor().getName() : null)
                .build();
    }

    @Override
    public ProductImageResponseDTO update(Long id, ProductImageRequestDTO dto) {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Product Image Not Found With ID: " + id));

        // Cập nhật Product
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        image.setProduct(product);

        // Cập nhật Variant nếu có
        ProductVariant variant = null;
        if (dto.getVariantId() != null && dto.getVariantId() != 0) {
            variant = variantRepository.findById(dto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));
        }
        image.setVariant(variant);

        // Cập nhật isMain
        image.setIsMain(dto.getIsMain() != null && dto.getIsMain());

        // Nếu có ảnh mới → upload
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String newImageUrl = uploadImage(dto.getImage());
            image.setImageUrl(newImageUrl);
        }

        ProductImage updated = productImageRepository.save(image);

        return ProductImageResponseDTO.builder()
                .id(updated.getId())
                .imageUrl(updated.getImageUrl())
                .isMain(updated.getIsMain())
                .productId(updated.getProduct().getId())
                .productName(updated.getProduct().getName())
                .variantId(updated.getVariant() != null ? updated.getVariant().getId() : null)
                .variantSize(updated.getVariant() != null && updated.getVariant().getSize() != null
                        ? updated.getVariant().getSize().getSizeName() : null)
                .variantColor(updated.getVariant() != null && updated.getVariant().getColor() != null
                        ? updated.getVariant().getColor().getName() : null)
                .build();
    }


    @Override
    public List<ProductImageResponseDTO> getByProductId(Long productId) {
        return productImageRepository.findByProductId(productId)
                .stream()
                .map(image -> ProductImageResponseDTO.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .isMain(image.getIsMain())
                        .productId(image.getProduct().getId())
                        .productName(image.getProduct().getName())
                        .variantId(image.getVariant() != null ? image.getVariant().getId() : null)
                        .variantSize(image.getVariant() != null && image.getVariant().getSize() != null
                                ? image.getVariant().getSize().getSizeName() : null)
                        .variantColor(image.getVariant() != null && image.getVariant().getColor() != null
                                ? image.getVariant().getColor().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public List<ProductImageResponseDTO> getByVariantId(Long variantId) {
        return productImageRepository.findByVariantId(variantId)
                .stream()
                .map(image -> ProductImageResponseDTO.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .isMain(image.getIsMain())
                        .productId(image.getProduct().getId())
                        .productName(image.getProduct().getName())
                        .variantId(image.getVariant() != null ? image.getVariant().getId() : null)
                        .variantSize(image.getVariant() != null && image.getVariant().getSize() != null
                                ? image.getVariant().getSize().getSizeName() : null)
                        .variantColor(image.getVariant() != null && image.getVariant().getColor() != null
                                ? image.getVariant().getColor().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public void delete(Long id) {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Product Image Not Found With ID: " + id));

        productImageRepository.delete(image);
    }
}
