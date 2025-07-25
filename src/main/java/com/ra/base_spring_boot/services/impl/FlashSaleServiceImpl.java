package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public List<FlashSale> getFlashSale() {

        return flashSaleRepository.findAll();
    }

    @Override
    public List<FlashSaleResponse> getFlashSaleDetails() {
        List<FlashSale> flashSales = flashSaleRepository.findAll();

        List<FlashSaleResponse> flashSaleResponses = new ArrayList<>();

        for (FlashSale flashSale : flashSales) {
            List<FlashSaleItem> flashSaleItems = flashSaleItemRepository.findByFlashSaleId(flashSale.getId());

            Map<Long, FlashSaleItem> flashSaleItemMap = flashSaleItems.stream()
                    .filter(item -> item.getVariant() != null)
                    .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

            Set<Product> flashSaleProducts = flashSaleItems.stream()
                    .map(item -> item.getVariant().getProduct())
                    .collect(Collectors.toSet());

            for (Product product : flashSaleProducts) {
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

                List<Review> reviews = reviewRepository.findAllByProduct(product);
                long totalReviews = reviews.size();
                double averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);

                FlashSaleItem flashSaleItem = product.getVariants().stream()
                        .map(variant -> flashSaleItemMap.get(variant.getId()))
                        .filter(item -> item != null)
                        .findFirst()
                        .orElse(null);

                FlashSaleResponse response = FlashSaleResponse.builder()
                        .id(product.getId())
                        .flashSaleName(flashSale.getName())
                        .flashSaleDescription(flashSale.getDescription())
                        .startTime(flashSale.getStartTime())
                        .endTime(flashSale.getEndTime())
                        .flashSaleStatus(flashSale.getStatus())
                        .productName(product.getName())
                        .productDescription(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .productStatus(product.getStatus())
                        .stockQuantity(totalStock)
                        .variantCount(variantDTOs.size())
                        .variants(variantDTOs)
                        .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                        .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                        .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                                ? product.getImages().get(0).getImageUrl()
                                : null)
                        .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                        .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                        .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                        .createdAt(product.getCreatedAt())
                        .averageRating(averageRating)
                        .totalReviews(totalReviews)
                        .isFlashSale(true)
                        .discountOverrideByFlashSale(flashSaleItem != null ? flashSaleItem.getDiscountedPrice() : null)
                        .discountType(flashSaleItem != null ? flashSaleItem.getDiscountType().name() : null)
                        .lowestPrice(variantDTOs.stream()
                                .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                                .filter(price -> price != null)
                                .min(BigDecimal::compareTo)
                                .orElse(product.getPrice()))
                        .discountedPrice(flashSaleItem != null ? flashSaleItem.getDiscountedPrice() : null)
                        .build();

                flashSaleResponses.add(response);
            }
        }

        return flashSaleResponses;
    }


    @Override
    public FlashSale save(FlashSale flashSale) {
        return flashSaleRepository.save(flashSale);
    }


}
