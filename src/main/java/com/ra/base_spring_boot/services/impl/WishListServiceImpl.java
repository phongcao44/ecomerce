package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.dto.resp.WishListResponse;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.DiscountType;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.services.IUserService;
import com.ra.base_spring_boot.services.IWishListService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishListServiceImpl implements IWishListService {
    private final IUserService iUserService;
    private final IWishListRepository iWishListRepository;
    private final IProductRepository iProductRepository;
    private final IReviewRepository iReviewRepository;
    private final IProductImageRepository iProductImageRepository;
    private final IFlashSaleRepository iflashSaleRepository;
    private final IFlashSaleItemRepository iFlashSaleItemRepository;

    public WishListServiceImpl(IProductRepository iProductRepository, IUserService iUserService, IWishListRepository iWishListRepository, IReviewRepository iReviewRepository, IProductImageRepository iProductImageRepository, IFlashSaleRepository iflashSaleRepository, IFlashSaleItemRepository iFlashSaleItemRepository) {
        this.iUserService = iUserService;
        this.iWishListRepository = iWishListRepository;
        this.iProductRepository = iProductRepository;
        this.iReviewRepository = iReviewRepository;
        this.iProductImageRepository = iProductImageRepository;
        this.iflashSaleRepository = iflashSaleRepository;
        this.iFlashSaleItemRepository = iFlashSaleItemRepository;
    }


    @Override
    public Wishlist findByWishlistId(long userID, Long wishlistId) {
        User user = iUserService.findUser(userID);
        try {
            return iWishListRepository.findByUserIdAndProduct_Id(userID, wishlistId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Wishlist addWishlist(long userID, long productId) {

        System.out.println(">> userID = " + userID);
        System.out.println(">> productId = " + productId);
        User user = iUserService.findUser(userID);
        Product product = iProductRepository.findById(productId).orElse(null);
        System.out.println("Product = " + product);


        if (user == null) {
            System.out.println("User not found");
            throw new RuntimeException("User not found");
        }
        if (product == null) {
            System.out.println("Product not found");
            throw new RuntimeException("Product not found");
        }

        Optional<Wishlist> existing = iWishListRepository.findByUserIdAndProduct_Id(userID, productId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishlist.setCreatedAt(LocalDateTime.now());


        return iWishListRepository.save(wishlist);
    }

    @Override
    public void deleteWishlist(long userID, long wishlistId) {
        Wishlist wishlist = iWishListRepository.findByIdAndUser_Id(userID, wishlistId).orElse(null);
        if (wishlist != null) {
            iWishListRepository.delete(wishlist);
        } else {
            throw new RuntimeException("Wishlist not found!");
        }
    }



    @Override
    public List<ProductResponseDTO> findAllWishlist(long userId) {
        List<Wishlist> wishlists = iWishListRepository.findAllByUser_Id(userId);

        // Lấy các Flash Sale đang hoạt động
        List<FlashSale> activeFlashSales = iflashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(LocalDateTime.now())
                        && flashSale.getEndTime().isAfter(LocalDateTime.now()))
                .toList();

        // Lấy tất cả Flash Sale Item của các Flash Sale đang active
        List<FlashSaleItem> activeFlashSaleItems = new ArrayList<>();
        for (FlashSale flashSale : activeFlashSales) {
            activeFlashSaleItems.addAll(iFlashSaleItemRepository.findByFlashSaleId(flashSale.getId()));
        }

        // Map Variant ID → FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Duyệt qua các Wishlist item
        return wishlists.stream().map(wishlist -> {
            Product product = wishlist.getProduct();

            // Lấy ảnh chính của sản phẩm (isMain = true)
            ProductImage mainImage = iProductImageRepository.findFirstByProductAndIsMainTrue(product)
                    .orElse(null);
            String productImgUrl = mainImage != null ? mainImage.getImageUrl() : null;

            // Tính tổng số review và điểm đánh giá trung bình
            List<Review> reviews = iReviewRepository.findAllByProduct(product);
            long totalReviews = reviews.size();
            double averageRating = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);

            // Xử lý Variants
            List<ProductVariant> variants = product.getVariants();
            int stockQuantity = variants.stream()
                    .mapToInt(ProductVariant::getStockQuantity)
                    .sum();

            // Map sang ProductVariantResponseDTO + áp dụng Flash Sale nếu có
            List<ProductVariantResponseDTO> variantDTOs = variants.stream().map(variant -> {
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
                        BigDecimal discountAmount = priceOriginal.multiply(percent)
                                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                        finalPrice = priceOriginal.subtract(discountAmount);
                    } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                        BigDecimal discountAmount = item.getDiscountedPrice();
                        finalPrice = priceOriginal.subtract(discountAmount);
                    }
                }

                variantDTO.setFinalPriceAfterDiscount(finalPrice);
                return variantDTO;
            }).collect(Collectors.toList());

            // Check if any variant is in a flash sale
            boolean isFlashSale = variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Calculate lowest price
            BigDecimal lowestPrice;
            if (isFlashSale) {
                lowestPrice = variantDTOs.stream()
                        .filter(dto -> flashSaleItemMap.containsKey(dto.getId()))
                        .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElseGet(() -> variantDTOs.stream()
                                .map(ProductVariantResponseDTO::getPriceOverride)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(product.getPrice()));
            } else {
                lowestPrice = variantDTOs.stream()
                        .map(ProductVariantResponseDTO::getPriceOverride)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(product.getPrice());
            }

            BigDecimal discountedPrice = variantDTOs.stream()
                    .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(lowestPrice);

            // Calculate discountOverrideByFlashSale and discountType for lowest flash sale price
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

            // Build ProductResponseDTO
            return ProductResponseDTO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .brand(product.getBrand())
                    .status(product.getStatus())
                    .stockQuantity(stockQuantity)
                    .variantCount(variants.size())
                    .variants(variantDTOs)
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .imageUrl(productImgUrl)
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
    }
}