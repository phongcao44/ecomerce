package com.ra.base_spring_boot.services.impl;


import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.dto.resp.FlashSaleVariantDetailResponse;
import com.ra.base_spring_boot.model.FlashSale;
import com.ra.base_spring_boot.model.FlashSaleItem;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.Review;
import com.ra.base_spring_boot.model.constants.DiscountType;
import com.ra.base_spring_boot.repository.IFlashSaleItemRepository;
import com.ra.base_spring_boot.repository.IFlashSaleRepository;
import com.ra.base_spring_boot.repository.IReviewRepository;
import com.ra.base_spring_boot.services.IFlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;

import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements IFlashSaleService {
    @Autowired
    private IFlashSaleRepository flashSaleRepository;
    @Autowired
    private IReviewRepository reviewRepository;
    @Autowired
    private IFlashSaleItemRepository flashSaleItemRepository;

    @Override
    public List<FlashSaleVariantDetailResponse> getFlashSaleItemsByFlashSaleId(Long flashSaleId) throws ChangeSetPersister.NotFoundException {
        // Kiểm tra xem flash sale có tồn tại không
        FlashSale flashSale = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException());

        // Lấy danh sách FlashSaleItem theo flashSaleId
        List<FlashSaleItem> flashSaleItems = flashSaleItemRepository.findByFlashSaleId(flashSaleId);

        // Tạo danh sách kết quả
        List<FlashSaleVariantDetailResponse> variantDetails = new ArrayList<>();

        // Map để lưu FlashSaleItem theo variant ID
        Map<Long, FlashSaleItem> flashSaleItemMap = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Lấy danh sách product từ FlashSaleItem
        Set<Product> flashSaleProducts = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null && item.getVariant().getProduct() != null)
                .map(item -> item.getVariant().getProduct())
                .collect(Collectors.toSet());

        // Duyệt qua từng product
        for (Product product : flashSaleProducts) {
            // Duyệt qua các variant của product
            if (product.getVariants() != null) {
                for (var variant : product.getVariants()) {
                    if (flashSaleItemMap.containsKey(variant.getId())) {
                        FlashSaleItem flashSaleItem = flashSaleItemMap.get(variant.getId());
                        BigDecimal priceOriginal = variant.getPriceOverride() != null ? variant.getPriceOverride() : BigDecimal.ZERO;
                        BigDecimal finalPrice = priceOriginal;

                        // Tính giá sau giảm giá
                        if (flashSaleItem.getDiscountType() == DiscountType.PERCENTAGE) {
                            BigDecimal percent = flashSaleItem.getDiscountedPrice();
                            BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100));
                            finalPrice = priceOriginal.subtract(discountAmount);
                        } else if (flashSaleItem.getDiscountType() == DiscountType.AMOUNT) {
                            BigDecimal discountAmount = flashSaleItem.getDiscountedPrice();
                            finalPrice = priceOriginal.subtract(discountAmount);
                        }

                        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                            finalPrice = BigDecimal.ZERO; // hoặc bạn có thể throw exception nếu muốn
                        }

                        // Tạo FlashSaleVariantDetailResponse
                        FlashSaleVariantDetailResponse variantDetail = FlashSaleVariantDetailResponse.builder()
                                .flashSaleItemId(flashSaleItem.getId())
                                .productId(product.getId()) // Added product ID
                                .variantId(variant.getId()) // Added variant ID
                                .productName(product.getName())
                                .color(variant.getColor() != null ? variant.getColor().getName() : null)
                                .size(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                                        ? product.getImages().get(0).getImageUrl()
                                        : null)
                                .originalPrice(priceOriginal)
                                .finalPrice(finalPrice)
                                .discountType(flashSaleItem.getDiscountType().name())
                                .discountedPrice(flashSaleItem.getDiscountedPrice())
                                .quantityLimit(variant.getStockQuantity())
                                .soldQuantity(flashSaleItem.getSoldQuantity() != null ? flashSaleItem.getSoldQuantity() : 0)
                                .build();

                        variantDetails.add(variantDetail);
                    }
                }
            }
        }

        return variantDetails;
    }

    @Override
    public List<FlashSale> getFlashSale() {
        List<FlashSale> flashSales = flashSaleRepository.findAll();
        List<FlashSale> result = new ArrayList<>();

        for (FlashSale flashSale : flashSales) {
            // Fetch FlashSaleItems for this FlashSale
            List<FlashSaleItem> flashSaleItems = flashSaleRepository.findFlashSaleItemById(flashSale.getId());

            // Calculate total unique products
            long totalProducts = flashSaleItems.stream()
                    .filter(item -> item.getVariant() != null && item.getVariant().getProduct() != null)
                    .map(item -> item.getVariant().getProduct().getId())
                    .distinct()
                    .count();

            // Set totalProducts on the FlashSale entity
            flashSale.setTotalProducts((int) totalProducts);

            result.add(flashSale);
        }

        return result;
    }

    @Override
    public List<ProductResponseDTO> getFlashSaleDetails(Long flashSaleId) {
        // Kiểm tra xem FlashSale có tồn tại không
        FlashSale flashSale = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Flash Sale với ID: " + flashSaleId));

        // Lấy danh sách FlashSaleItem theo flashSaleId
        List<FlashSaleItem> flashSaleItems = flashSaleItemRepository.findByFlashSaleId(flashSaleId);
        if (flashSaleItems.isEmpty()) {
            return new ArrayList<>();
        }

        // Tạo map từ variantId đến FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Lấy danh sách sản phẩm từ FlashSaleItem
        Set<Product> flashSaleProducts = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null && item.getVariant().getProduct() != null)
                .map(item -> item.getVariant().getProduct())
                .collect(Collectors.toSet());

        // Map sản phẩm sang ProductResponseDTO
        List<ProductResponseDTO> productResponses = flashSaleProducts.stream().map(product -> {
            List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                    ? product.getVariants().stream()
                    .map(variant -> {
                        BigDecimal priceOriginal = variant.getPriceOverride() != null ? variant.getPriceOverride() : BigDecimal.ZERO;
                        BigDecimal finalPrice = priceOriginal;

                        ProductVariantResponseDTO variantDTO = ProductVariantResponseDTO.builder()
                                .id(variant.getId())
                                .productName(product.getName())
                                .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                                .stockQuantity(variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                                .priceOverride(priceOriginal)
                                .build();

                        // Áp dụng giảm giá nếu biến thể thuộc FlashSale
                        if (flashSaleItemMap.containsKey(variant.getId())) {
                            FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                            variantDTO.setDiscountOverrideByFlashSale(item.getDiscountedPrice());
                            variantDTO.setDiscountType(item.getDiscountType() != null ? item.getDiscountType().name() : null);

                            if (item.getDiscountType() == DiscountType.PERCENTAGE && item.getDiscountedPrice() != null) {
                                BigDecimal percent = item.getDiscountedPrice();
                                BigDecimal discountAmount = priceOriginal.multiply(percent)
                                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                                finalPrice = priceOriginal.subtract(discountAmount);
                            } else if (item.getDiscountType() == DiscountType.AMOUNT && item.getDiscountedPrice() != null) {
                                BigDecimal discountAmount = item.getDiscountedPrice();
                                finalPrice = priceOriginal.subtract(discountAmount);
                            }
                        }

                        variantDTO.setFinalPriceAfterDiscount(finalPrice);
                        return variantDTO;
                    }).collect(Collectors.toList())
                    : new ArrayList<>();

            // Tính tổng số lượng tồn kho
            int totalStock = variantDTOs.stream()
                    .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                    .sum();

            // Tính đánh giá trung bình và số lượng đánh giá
            List<Review> reviews = reviewRepository.findAllByProduct(product);
            long totalReviews = reviews.size();
            double averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);

            // Kiểm tra xem sản phẩm có thuộc FlashSale không
            boolean isFlashSale = !variantDTOs.isEmpty() && variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Tính giá thấp nhất
            BigDecimal lowestPrice = variantDTOs.stream()
                    .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

            // Tính discountedPrice (giá sau khi áp dụng giảm giá)
            BigDecimal discountedPrice = variantDTOs.stream()
                    .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(lowestPrice);

            // Lấy discountOverrideByFlashSale và discountType từ variant có giá thấp nhất trong FlashSale
            BigDecimal discountOverrideByFlashSale = null;
            String discountType = null;
            if (isFlashSale) {
                ProductVariantResponseDTO lowestFlashSaleVariant = variantDTOs.stream()
                        .filter(dto -> flashSaleItemMap.containsKey(dto.getId()))
                        .filter(dto -> dto.getFinalPriceAfterDiscount() != null)
                        .min(Comparator.comparing(ProductVariantResponseDTO::getFinalPriceAfterDiscount))
                        .orElse(null);
                if (lowestFlashSaleVariant != null) {
                    discountOverrideByFlashSale = lowestFlashSaleVariant.getDiscountOverrideByFlashSale();
                    discountType = lowestFlashSaleVariant.getDiscountType();
                }
            }

            return ProductResponseDTO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                    .brand(product.getBrand())
                    .status(product.getStatus())
                    .stockQuantity(totalStock)
                    .variantCount(product.getVariants() != null ? product.getVariants().size() : 0) // Đếm tổng số biến thể
                    .variants(variantDTOs)
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0).getImageUrl() : null)
                    .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                    .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                    .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                    .createdAt(product.getCreatedAt())
                    .averageRating(averageRating)
                    .totalReviews(totalReviews)
                    .lowestPrice(lowestPrice)
                    .discountedPrice(discountedPrice)
                    .isFlashSale(isFlashSale)
                    .discountOverrideByFlashSale(discountOverrideByFlashSale)
                    .discountType(discountType)
                    .build();
        }).collect(Collectors.toList());

        return productResponses;
    }

    @Override
    public FlashSale save(FlashSale flashSale) {
        return flashSaleRepository.save(flashSale);
    }
}