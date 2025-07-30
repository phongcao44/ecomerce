package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.FlashSaleResponse;
import com.ra.base_spring_boot.dto.resp.ListProductReviewResponse;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.repository.IFlashSaleItemRepository;
import com.ra.base_spring_boot.repository.IFlashSaleRepository;
import com.ra.base_spring_boot.repository.IRateRepository;
import com.ra.base_spring_boot.repository.IReviewRepository;
import com.ra.base_spring_boot.services.IFlashSaleItemService;
import com.ra.base_spring_boot.services.IFlashSaleService;
import com.ra.base_spring_boot.services.IRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FlashSaleItemServiceImpl implements IFlashSaleItemService {
    @Autowired
    private IFlashSaleItemRepository flashSaleItemRepository;
    @Autowired
    private IRateService reviewRepo;
    @Autowired
    private IRateRepository reviewRepository;
    @Autowired
    private IFlashSaleRepository flashSaleRepository;
    @Override
    public FlashSaleItem save(FlashSaleItem flashSaleItem) {
        return flashSaleItemRepository.save(flashSaleItem);
    }

    @Override
    public FlashSaleResponse getTop1() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm FlashSale đang hoạt động
        FlashSale flashSale = flashSaleRepository
                .findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)
                .orElseThrow(() -> new RuntimeException("Không có Flash Sale đang hoạt động"));

        // 2. Lấy sản phẩm có giảm giá nhiều nhất trong Flash Sale đó
        FlashSaleItem topItem = flashSaleItemRepository.findTopDiscountItemByFlashSaleId(flashSale.getId());
        if (topItem == null) {
            throw new RuntimeException("Không có sản phẩm trong Flash Sale");
        }

        Product product = topItem.getProduct();
        ProductVariant variant = topItem.getVariant();

        // 3. Tìm đánh giá của sản phẩm (nếu có)
        ListProductReviewResponse reviewProduct = reviewRepository.findReviewSummariesGroupedByProduct()
                .stream()
                .filter(r -> r.getProductId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        // 4. Lấy ảnh sản phẩm (nếu có)
        String imageUrl = product.getImages().get(0).getImageUrl();

        // 5. Xây dựng response
        return FlashSaleResponse.builder()
                .productId(product.getId())
                .id(flashSale.getId())
                .flashSaleName(flashSale.getName())
                .flashSaleDescription(flashSale.getDescription())
                .startTime(flashSale.getStartTime())
                .endTime(flashSale.getEndTime())
                .flashSaleStatus(flashSale.getStatus())

                .productName(product.getName())
                .productDescription(product.getDescription())
                .price(variant.getPriceOverride())
                .discountedPrice(topItem.getDiscountedPrice())
                .lowestPrice(variant.getPriceOverride().min(topItem.getDiscountedPrice()))
                .discountOverrideByFlashSale(variant.getPriceOverride().subtract(topItem.getDiscountedPrice()))
                .discountType(topItem.getDiscountType().name())
                .brand(product.getBrand())
                .productStatus(product.getStatus())
                .stockQuantity(variant.getStockQuantity())
                .variantCount(product.getVariants().size())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .imageUrl(imageUrl)
                .createdAt(product.getCreatedAt())
                .returnPolicyId(product.getReturnPolicy().getId())
                .returnPolicyTitle(product.getReturnPolicy().getTitle())
                .returnPolicyContent(product.getReturnPolicy().getContent())
                .averageRating(reviewRepo.getAverageRatingByProductId(product.getId()))
                .totalReviews(reviewProduct != null ? reviewProduct.getTotalReviews() : 0)
                .isFlashSale(true)
                .build();
    }

}
