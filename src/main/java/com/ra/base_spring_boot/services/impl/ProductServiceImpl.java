package com.ra.base_spring_boot.services.impl;


import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.exception.HttpForbiden;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.dto.resp.Top5Product;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.DiscountType;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.ProductStatus;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final IReturnPolicyRepository returnPolicyRepository;

    @Autowired
    private IColorRepository colorRepository;
    @Autowired
    private ISizeRepository sizeRepository;
    @Autowired
    private IFlashSaleItemRepository  flashSaleItemRepository;
    @Autowired
    private IFlashSaleRepository flashSaleRepository;


    @Override
    public List<ProductResponseDTO> findAll() {
        // Lấy các flash sale đang hoạt động (giống như findById nếu muốn áp giá giảm)
        List<FlashSale> activeFlashSales = flashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(LocalDateTime.now())
                        && flashSale.getEndTime().isAfter(LocalDateTime.now()))
                .toList();

        List<FlashSaleItem> activeFlashSaleItems = new ArrayList<>();
        for (FlashSale flashSale : activeFlashSales) {
            activeFlashSaleItems.addAll(flashSaleItemRepository.findByFlashSaleId(flashSale.getId()));
        }

        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product -> {
                    List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                            ? product.getVariants().stream().map(variant -> {
                        BigDecimal priceOriginal = variant.getPriceOverride();
                        BigDecimal finalPrice = priceOriginal;

                        ProductVariantResponseDTO variantDTO = ProductVariantResponseDTO.builder()
                                .id(variant.getId())
                                .productName(product.getName())
                                .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                                .stockQuantity(variant.getStockQuantity())
                                .priceOverride(priceOriginal)
                                .build();

                        if (flashSaleItemMap.containsKey(variant.getId())) {
                            FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                            variantDTO.setDiscountOverrideByFlashSale(item.getDiscountedPrice());
                            variantDTO.setDiscountType(item.getDiscountType().name());

                            if (item.getDiscountType() == DiscountType.PERCENTAGE) {
                                BigDecimal percent = item.getDiscountedPrice();
                                BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100));
                                finalPrice = priceOriginal.subtract(discountAmount);
                            } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                                BigDecimal discountAmount = item.getDiscountedPrice();
                                finalPrice = priceOriginal.subtract(discountAmount);
                            }
                        }

                        variantDTO.setFinalPriceAfterDiscount(finalPrice);
                        return variantDTO;
                    }).collect(Collectors.toList())
                            : new ArrayList<>();

                    int totalStock = variantDTOs.stream()
                            .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                            .sum();

                    return ProductResponseDTO.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .brand(product.getBrand())
                            .status(product.getStatus())
                            .stockQuantity(totalStock)
                            .variantCount(variantDTOs.size())
                            .variants(variantDTOs)
                            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                            .categoryName(product.getCategory().getName())
                            .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                                    ? product.getImages().get(0).getImageUrl()
                                    : null)
                            .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                            .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                            .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                            .createdAt(product.getCreatedAt())
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    public ProductResponseDTO findById(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        // Lấy flash sale đang hoạt động
        List<FlashSale> activeFlashSales = flashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(LocalDateTime.now())
                        && flashSale.getEndTime().isAfter(LocalDateTime.now()))
                .toList();

        // Lấy danh sách item trong flash sale
        List<FlashSaleItem> activeFlashSaleItems = new ArrayList<>();
        for (FlashSale flashSale : activeFlashSales) {
            activeFlashSaleItems.addAll(flashSaleItemRepository.findByFlashSaleId(flashSale.getId()));
        }

        // Map variantId -> FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Map các variant sang DTO
        List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                ? product.getVariants().stream().map(variant -> {
            BigDecimal priceOriginal = variant.getPriceOverride();
            BigDecimal finalPrice = priceOriginal;

            ProductVariantResponseDTO dto = ProductVariantResponseDTO.builder()
                    .id(variant.getId())
                    .productName(product.getName())
                    .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                    .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                    .stockQuantity(variant.getStockQuantity())
                    .priceOverride(priceOriginal)
                    .build();

            if (flashSaleItemMap.containsKey(variant.getId())) {
                FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                dto.setDiscountOverrideByFlashSale(item.getDiscountedPrice());
                dto.setDiscountType(item.getDiscountType().name());

                // Tính giá sau giảm
                if (item.getDiscountType() == DiscountType.PERCENTAGE) {
                    BigDecimal percent = item.getDiscountedPrice();
                    BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100));
                    finalPrice = priceOriginal.subtract(discountAmount);
                } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                    finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                }
            }

            dto.setFinalPriceAfterDiscount(finalPrice);
            return dto;
        }).collect(Collectors.toList())
                : null;

        int totalStock = product.getVariants() != null
                ? product.getVariants().stream()
                .mapToInt(variant -> variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                .sum()
                : 0;

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .status(product.getStatus())
                .brand(product.getBrand())
                .stockQuantity(totalStock)
                .variantCount(variantDTOs != null ? variantDTOs.size() : 0)
                .categoryName(product.getCategory().getName())
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl()
                        : null)
                .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                .variants(variantDTOs)
                .createdAt(product.getCreatedAt())
                .build();
    }
        @Override
    public ProductResponseDTO save(ProductRequestDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new HttpNotFound("Category Not Found"));

        ReturnPolicy returnPolicy = returnPolicyRepository.findById(dto.getReturn_policy_id())
                .orElseThrow(() -> new HttpNotFound("Return Policy Not Found"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .brand(dto.getBrand())
                .status(dto.getStatus())
                .category(category)
                .returnPolicy(returnPolicy)
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
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl()
                        : null)
                .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                .build();
    }


    @Override
    public ProductResponseDTO changeStatus(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        // Chỉ chuyển đổi giữa Active và InActive
        if (product.getStatus() == ProductStatus.IN_STOCK) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.IN_STOCK);
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
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl()
                        : null)
                .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                .build();

    }


    @Override
    public ProductResponseDTO update(long id, ProductRequestDTO dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Product Not Found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new HttpNotFound("Category Not Found"));

        ReturnPolicy returnPolicy = returnPolicyRepository.findById(dto.getReturn_policy_id())
                .orElseThrow(() -> new HttpNotFound("Return Policy Not Found"));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setBrand(dto.getBrand());
        existing.setStatus(dto.getStatus());
        existing.setCategory(category);
        existing.setReturnPolicy(returnPolicy);

        Product updated = productRepository.save(existing);

        return ProductResponseDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .brand(updated.getBrand())
                .status(updated.getStatus())
                .categoryName(updated.getCategory().getName())
                .imageUrl(updated.getImages() != null && !updated.getImages().isEmpty()
                        ? updated.getImages().get(0).getImageUrl()
                        : null)
                .returnPolicyId(updated.getReturnPolicy() != null ? updated.getReturnPolicy().getId() : null)
                .returnPolicyTitle(updated.getReturnPolicy() != null ? updated.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(updated.getReturnPolicy() != null ? updated.getReturnPolicy().getContent() : null)
                .build();
    }


    @Override
    public Page<ProductResponseDTO> pagination(Pageable pageable, String keyword, String status) {
        Page<Product> productPage;

        // Chuyển đổi status (String) thành Enum
        ProductStatus productStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                productStatus = ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                try {
                    throw new BadRequestException("Trạng thái sản phẩm không hợp lệ: " + status);
                } catch (BadRequestException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        // Nếu có cả keyword và status
        if (keyword != null && !keyword.isBlank() && productStatus != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, productStatus, pageable);
        }
        // Chỉ có keyword
        else if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }
        // Chỉ có status
        else if (productStatus != null) {
            productPage = productRepository.findByStatus(productStatus, pageable);
        }
        // Không có filter
        else {
            productPage = productRepository.findAll(pageable);
        }

        // Mapping Product -> ProductResponseDTO
        return productPage.map(product -> {
            int totalStock = product.getVariants() != null
                    ? product.getVariants().stream()
                    .mapToInt(variant -> variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                    .sum()
                    : 0;

            return ProductResponseDTO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .brand(product.getBrand())
                    .status(product.getStatus())
                    .stockQuantity(totalStock)
                    .variantCount(product.getVariants() != null ? product.getVariants().size() : 0)
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory().getName())
                    .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0).getImageUrl()
                            : null)
                    .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                    .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                    .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                    .build();
        });
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
            throw new HttpForbiden("This product has variations you need to delete the variations first");
        }
        productRepository.deleteById(id);
    }

    @Override
    public List<Top5Product> getTop5BestSellingProducts() {
        List<Object[]> result = productRepository.findTop5BestSellingProducts(
                OrderStatus.DELIVERED,
                PageRequest.of(0, 5)
        );

        return result.stream().map(row -> {
            Product product = (Product) row[0];
            Long purchaseCount = (Long) row[1];
            Double avgRating = (Double) row[2];
            Long totalView = (Long) row[3];

            return Top5Product.builder()
                    .id(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice().doubleValue())
                    .purchaseCount(purchaseCount)
                    .averageRating(avgRating)
                    .totalReviews(totalView) // dùng lại field này cho view nếu bạn không tách riêng
                    .image(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl())
                    .build();
        }).toList();
    }

    @Override
    public List<Top5Product> getTop5LestSellingProducts() {
        List<Object[]> result = productRepository.findTop5LeastSellingWithRatingAndView(
                OrderStatus.DELIVERED,
                PageRequest.of(0, 5)
        );

        return result.stream().map(row -> {
            Product product = (Product) row[0];
            Long purchaseCount = (Long) row[1];
            Double avgRating = (Double) row[2];
            Long totalView = (Long) row[3];

            return Top5Product.builder()
                    .id(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice().doubleValue())
                    .purchaseCount(purchaseCount)
                    .averageRating(avgRating)
                    .totalReviews(totalView) // dùng lại field này cho view nếu bạn không tách riêng
                    .image(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl())
                    .build();
        }).toList();
    }
}