package com.ra.base_spring_boot.services.impl;


import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.dto.resp.FlashSaleVariantDetailResponse;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.DiscountType;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IFlashSaleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;

import java.math.BigDecimal;
import java.text.Normalizer;
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

    private final IOrderItemRepository orderItemRepository;

    @Autowired
    private final ICategoryRepository categoryRepository;

    @Autowired
    private final IProductRepository productRepository;

    private final IWishListRepository wishListRepository;

    private static final Logger log = LoggerFactory.getLogger(FlashSaleServiceImpl.class);

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

    // Phân trang bộ lọc cho flash sale
    @Override
    public Page<ProductResponseDTO> getFlashSaleItemsPaginate(
            Long flashSaleId,
            Long categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String discountRange,
            Integer minRating,
            int page,
            int limit,
            String sortBy,
            String orderBy) {

        // Validate inputs
        if (flashSaleId == null) {
            throw new IllegalArgumentException("Flash Sale ID cannot be null");
        }

        // Check if FlashSale exists and is currently active
        FlashSale flashSale = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Flash Sale với ID: " + flashSaleId));

        // Validate flash sale time period
        LocalDateTime now = LocalDateTime.now();
        if (flashSale.getStartTime() != null && now.isBefore(flashSale.getStartTime())) {
            throw new RuntimeException("Flash Sale chưa bắt đầu. Thời gian bắt đầu: " + flashSale.getStartTime());
        }
        if (flashSale.getEndTime() != null && now.isAfter(flashSale.getEndTime())) {
            throw new RuntimeException("Flash Sale đã kết thúc. Thời gian kết thúc: " + flashSale.getEndTime());
        }

        // Get FlashSaleItems
        List<FlashSaleItem> flashSaleItems = flashSaleItemRepository.findByFlashSaleId(flashSaleId);
        if (flashSaleItems.isEmpty()) {
            log.info("No FlashSaleItems found for Flash Sale ID {}", flashSaleId);
            return new PageImpl<ProductResponseDTO>(new ArrayList<ProductResponseDTO>(), PageRequest.of(page, limit), 0);
        }

        // Create map from variantId to FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Get products from FlashSaleItem
        Set<Product> flashSaleProducts = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null && item.getVariant().getProduct() != null)
                .map(item -> item.getVariant().getProduct())
                .collect(Collectors.toSet());

        if (flashSaleProducts.isEmpty()) {
            log.info("No products found for Flash Sale ID {}", flashSaleId);
            return new PageImpl<ProductResponseDTO>(new ArrayList<ProductResponseDTO>(), PageRequest.of(page, limit), 0);
        }

        // Get logged-in user ID
        final Long userId = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal())
                .filter(principal -> principal instanceof MyUserDetails)
                .map(principal -> ((MyUserDetails) principal).getUser().getId())
                .orElse(null);

        // Fetch wishlist entries
        List<Long> productIds = flashSaleProducts.stream()
                .map(Product::getId)
                .toList();
        List<Wishlist> wishlists = (userId != null && !productIds.isEmpty())
                ? wishListRepository.findAllByUser_IdAndProduct_IdIn(userId, productIds)
                : List.of();
        Set<Long> favoriteProductIds = wishlists.stream()
                .map(wishlist -> wishlist.getProduct().getId())
                .collect(Collectors.toSet());

        // Fetch order items for variant-level sold quantities
        List<Long> variantIds = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .map(item -> item.getVariant().getId())
                .toList();
        List<OrderItem> orderItems = variantIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByVariantIdIn(variantIds);
        Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // Map products to DTOs and apply filters
        List<ProductResponseDTO> allProducts = flashSaleProducts.stream()
                .map(product -> mapToProductResponseDTO(product, flashSaleItemMap, variantSoldQuantities, userId, favoriteProductIds))
                .filter(product -> applyFilters(product, categoryId, brand, minPrice, maxPrice, discountRange, minRating))
                .collect(Collectors.toList());

        // Apply sorting
        allProducts = applySorting(allProducts, sortBy, orderBy);

        // Apply pagination
        int totalElements = allProducts.size();
        int startIndex = page * limit;
        int endIndex = Math.min(startIndex + limit, totalElements);

        List<ProductResponseDTO> paginatedProducts = startIndex < totalElements
                ? allProducts.subList(startIndex, endIndex)
                : new ArrayList<ProductResponseDTO>();

        return new PageImpl<ProductResponseDTO>(paginatedProducts, PageRequest.of(page, limit), totalElements);
    }

    private ProductResponseDTO mapToProductResponseDTO(Product product,
                                                       Map<Long, FlashSaleItem> flashSaleItemMap,
                                                       Map<Long, Integer> variantSoldQuantities,
                                                       Long userId,
                                                       Set<Long> favoriteProductIds) {

        // Fetch sold quantity for the product
        Integer soldQuantity = orderItemRepository.countSoldQuantityByProductId(product.getId());
        if (soldQuantity == null) {
            soldQuantity = 0;
        }

        // Map product variants to DTOs
        List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                ? product.getVariants().stream()
                .map(variant -> {
                    BigDecimal priceOriginal = variant.getPriceOverride() != null ? variant.getPriceOverride() : BigDecimal.ZERO;
                    BigDecimal finalPrice = priceOriginal;
                    BigDecimal discountValue = BigDecimal.ZERO;
                    String discountType = null;

                    if (flashSaleItemMap.containsKey(variant.getId())) {
                        FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                        discountValue = item.getDiscountedPrice() != null ? item.getDiscountedPrice() : BigDecimal.ZERO;
                        discountType = item.getDiscountType() != null ? item.getDiscountType().name() : null;

                        if (item.getDiscountType() == DiscountType.PERCENTAGE && item.getDiscountedPrice() != null) {
                            BigDecimal percent = item.getDiscountedPrice();
                            BigDecimal discountAmount = priceOriginal.multiply(percent)
                                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                            finalPrice = priceOriginal.subtract(discountAmount);
                        } else if (item.getDiscountType() == DiscountType.AMOUNT && item.getDiscountedPrice() != null) {
                            finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                        }
                    }

                    return ProductVariantResponseDTO.builder()
                            .id(variant.getId())
                            .sku(variant.getSku())
                            .barcode(variant.getBarcode())
                            .productName(product.getName())
                            .colorId(variant.getColor() != null ? variant.getColor().getId() : null)
                            .sizeId(variant.getSize() != null ? variant.getSize().getId() : null)
                            .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                            .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                            .stockQuantity(variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                            .priceOverride(priceOriginal)
                            .discountOverrideByFlashSale(finalPrice)
                            .discountType(discountType)
                            .soldQuantity(variantSoldQuantities.getOrDefault(variant.getId(), 0))
                            .finalPriceAfterDiscount(finalPrice)
                            .build();
                }).collect(Collectors.toList())
                : new ArrayList<ProductVariantResponseDTO>();

        // Calculate total stock
        int totalStock = variantDTOs.stream()
                .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                .sum();

        // Calculate average rating and review count
        List<Review> reviews = reviewRepository.findAllByProduct(product);
        long totalReviews = reviews.size();
        double averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);

        // Check if product is in flash sale
        boolean isFlashSale = !variantDTOs.isEmpty() && variantDTOs.stream()
                .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

        // Calculate lowest price
        BigDecimal lowestPrice = variantDTOs.stream()
                .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

        // Calculate discount details from lowest price variant
        BigDecimal discountedPrice = null;
        BigDecimal originalPrice = null;
        BigDecimal discountOverrideByFlashSale = null;
        String discountType = null;
        if (isFlashSale) {
            ProductVariantResponseDTO lowestFlashSaleVariant = variantDTOs.stream()
                    .filter(dto -> flashSaleItemMap.containsKey(dto.getId()))
                    .filter(dto -> dto.getFinalPriceAfterDiscount() != null)
                    .min(Comparator.comparing(ProductVariantResponseDTO::getFinalPriceAfterDiscount))
                    .orElse(null);
            if (lowestFlashSaleVariant != null) {
                FlashSaleItem item = flashSaleItemMap.get(lowestFlashSaleVariant.getId());
                discountedPrice = item.getDiscountedPrice() != null ? item.getDiscountedPrice() : BigDecimal.ZERO;
                originalPrice = lowestFlashSaleVariant.getPriceOverride();
                discountOverrideByFlashSale = lowestFlashSaleVariant.getFinalPriceAfterDiscount();
                discountType = lowestFlashSaleVariant.getDiscountType();
            }
        }

        // Generate slug and check favorite status
        String slug = product.getSlug() != null ? product.getSlug() : generateSlug(product.getName());
        Boolean isFavorite = userId != null && favoriteProductIds.contains(product.getId());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                .lowestPrice(lowestPrice)
                .brand(product.getBrand())
                .isFlashSale(isFlashSale)
                .discountedPrice(discountedPrice)
                .originalPrice(originalPrice)
                .discountOverrideByFlashSale(discountOverrideByFlashSale)
                .discountType(discountType)
                .soldQuantity(soldQuantity)
                .slug(slug)
                .isFavorite(isFavorite)
                .updatedAt(product.getUpdatedAt())
                .status(product.getStatus())
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .stockQuantity(totalStock)
                .variantCount(variantDTOs.size())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl() : null)
                .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                .createdAt(product.getCreatedAt())
                .variants(variantDTOs)
                .build();
    }

    private boolean applyFilters(ProductResponseDTO product,
                                 Long categoryId,
                                 String brand,
                                 BigDecimal minPrice,
                                 BigDecimal maxPrice,
                                 String discountRange,
                                 Integer minRating) {

        // Category filter with child categories support
        if (categoryId != null) {
            List<Long> categoryIds = getAllChildCategoryIds(categoryId);
            if (product.getCategoryId() == null || !categoryIds.contains(product.getCategoryId())) {
                return false;
            }
        }

        // Brand filter
        if (brand != null && !brand.trim().isEmpty() &&
                (product.getBrand() == null || !product.getBrand().toLowerCase().contains(brand.toLowerCase().trim()))) {
            return false;
        }

        // Price range filter
        BigDecimal productPrice = product.getLowestPrice();
        if (minPrice != null && productPrice.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && productPrice.compareTo(maxPrice) > 0) {
            return false;
        }

        // Discount range filter
        if (discountRange != null && !discountRange.trim().isEmpty() && product.isFlashSale()) {
            if (!matchesDiscountRange(product, discountRange)) {
                return false;
            }
        }

        // Rating filter
        if (minRating != null && product.getAverageRating() < minRating) {
            return false;
        }

        return true;
    }

    private List<Long> getAllChildCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId); // bản thân nó

        List<Category> children = categoryRepository.findByParentId(categoryId);
        for (Category child : children) {
            ids.addAll(getAllChildCategoryIds(child.getId()));
        }

        return ids;
    }

    private boolean matchesDiscountRange(ProductResponseDTO product, String discountRange) {
        if (product.getOriginalPrice() == null || product.getDiscountOverrideByFlashSale() == null) {
            return false;
        }

        BigDecimal originalPrice = product.getOriginalPrice();
        BigDecimal discountedPrice = product.getDiscountOverrideByFlashSale();

        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal discountAmount = originalPrice.subtract(discountedPrice);
        BigDecimal discountPercentage = discountAmount.divide(originalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        double discountPercent = discountPercentage.doubleValue();

        switch (discountRange.toLowerCase()) {
            case "0-10":
                return discountPercent >= 0 && discountPercent <= 10;
            case "10-25":
                return discountPercent > 10 && discountPercent <= 25;
            case "25-40":
                return discountPercent > 25 && discountPercent <= 40;
            case "40-60":
                return discountPercent > 40 && discountPercent <= 60;
            case "60+":
                return discountPercent > 60;
            default:
                return true;
        }
    }

    private List<ProductResponseDTO> applySorting(List<ProductResponseDTO> products, String sortBy, String orderBy) {
        Comparator<ProductResponseDTO> comparator;

        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(p -> p.getName() != null ? p.getName() : "");
                break;
            case "price":
                comparator = Comparator.comparing(ProductResponseDTO::getLowestPrice);
                break;
            case "rating":
                comparator = Comparator.comparing(ProductResponseDTO::getAverageRating);
                break;
            case "soldquantity":
                comparator = Comparator.comparing(p -> p.getSoldQuantity() != null ? p.getSoldQuantity() : 0);
                break;
            case "createdat":
            default:
                comparator = Comparator.comparing(p -> p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.MIN);
                break;
        }

        if ("desc".equalsIgnoreCase(orderBy)) {
            comparator = comparator.reversed();
        }

        return products.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }




    @Override
    public List<ProductResponseDTO> getFlashSaleDetails(Long flashSaleId) {
        // Kiểm tra flashSaleId
        if (flashSaleId == null) {
            throw new IllegalArgumentException("Flash Sale ID cannot be null");
        }

        // Kiểm tra xem FlashSale có tồn tại không
        FlashSale flashSale = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Flash Sale với ID: " + flashSaleId));

        // Lấy danh sách FlashSaleItem theo flashSaleId
        List<FlashSaleItem> flashSaleItems = flashSaleItemRepository.findByFlashSaleId(flashSaleId);
        if (flashSaleItems.isEmpty()) {
            log.info("No FlashSaleItems found for Flash Sale ID {}", flashSaleId);
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

        if (flashSaleProducts.isEmpty()) {
            log.info("No products found for Flash Sale ID {}", flashSaleId);
            return new ArrayList<>();
        }

        // Get logged-in user's ID from SecurityContextHolder
        final Long userId = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal())
                .filter(principal -> principal instanceof MyUserDetails)
                .map(principal -> ((MyUserDetails) principal).getUser().getId())
                .orElse(null);

        // Fetch wishlist entries for the products
        List<Long> productIds = flashSaleProducts.stream()
                .map(Product::getId)
                .toList();
        List<Wishlist> wishlists = (userId != null && !productIds.isEmpty())
                ? wishListRepository.findAllByUser_IdAndProduct_IdIn(userId, productIds)
                : List.of();
        Set<Long> favoriteProductIds = wishlists.stream()
                .map(wishlist -> wishlist.getProduct().getId())
                .collect(Collectors.toSet());

        // Fetch order items for variant-level sold quantities
        List<Long> variantIds = flashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .map(item -> item.getVariant().getId())
                .toList();
        List<OrderItem> orderItems = variantIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByVariantIdIn(variantIds);
        Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // Map sản phẩm sang ProductResponseDTO
        return flashSaleProducts.stream().map(product -> {
            // Fetch sold quantity for the product
            Integer soldQuantity = orderItemRepository.countSoldQuantityByProductId(product.getId());
            if (soldQuantity == null) {
                soldQuantity = 0;
            }

            // Map product variants to DTOs
            List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                    ? product.getVariants().stream()
                    .map(variant -> {
                        BigDecimal priceOriginal = variant.getPriceOverride() != null ? variant.getPriceOverride() : BigDecimal.ZERO;
                        BigDecimal finalPrice = priceOriginal;
                        BigDecimal discountValue = BigDecimal.ZERO; // Giá trị giảm giá (phần trăm hoặc số tiền)
                        String discountType = null;

                        if (flashSaleItemMap.containsKey(variant.getId())) {
                            FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                            discountValue = item.getDiscountedPrice() != null ? item.getDiscountedPrice() : BigDecimal.ZERO;
                            discountType = item.getDiscountType() != null ? item.getDiscountType().name() : null;

                            if (item.getDiscountType() == DiscountType.PERCENTAGE && item.getDiscountedPrice() != null) {
                                BigDecimal percent = item.getDiscountedPrice();
                                BigDecimal discountAmount = priceOriginal.multiply(percent)
                                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                                finalPrice = priceOriginal.subtract(discountAmount);
                            } else if (item.getDiscountType() == DiscountType.AMOUNT && item.getDiscountedPrice() != null) {
                                finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                            }
                        }

                        return ProductVariantResponseDTO.builder()
                                .id(variant.getId())
                                .sku(variant.getSku())
                                .barcode(variant.getBarcode())
                                .productName(product.getName())
                                .colorId(variant.getColor() != null ? variant.getColor().getId() : null)
                                .sizeId(variant.getSize() != null ? variant.getSize().getId() : null)
                                .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                                .stockQuantity(variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                                .priceOverride(priceOriginal)
                                .discountOverrideByFlashSale(finalPrice) // Giá cuối cùng
                                .discountType(discountType)
                                .soldQuantity(variantSoldQuantities.getOrDefault(variant.getId(), 0))
                                .finalPriceAfterDiscount(finalPrice)
                                .build();
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

            // Tính discountedPrice, originalPrice, discountOverrideByFlashSale từ biến thể có giá giảm thấp nhất
            BigDecimal discountedPrice = null;
            BigDecimal originalPrice = null;
            BigDecimal discountOverrideByFlashSale = null;
            String discountType = null;
            if (isFlashSale) {
                ProductVariantResponseDTO lowestFlashSaleVariant = variantDTOs.stream()
                        .filter(dto -> flashSaleItemMap.containsKey(dto.getId()))
                        .filter(dto -> dto.getFinalPriceAfterDiscount() != null)
                        .min(Comparator.comparing(ProductVariantResponseDTO::getFinalPriceAfterDiscount))
                        .orElse(null);
                if (lowestFlashSaleVariant != null) {
                    FlashSaleItem item = flashSaleItemMap.get(lowestFlashSaleVariant.getId());
                    discountedPrice = item.getDiscountedPrice() != null ? item.getDiscountedPrice() : BigDecimal.ZERO; // Phần trăm hoặc số tiền giảm
                    originalPrice = lowestFlashSaleVariant.getPriceOverride(); // Giá gốc của biến thể
                    discountOverrideByFlashSale = lowestFlashSaleVariant.getFinalPriceAfterDiscount(); // Giá cuối cùng
                    discountType = lowestFlashSaleVariant.getDiscountType();
                }
            }

            // Derive slug and isFavorite
            String slug = product.getSlug() != null ? product.getSlug() : generateSlug(product.getName());
            Boolean isFavorite = userId != null && favoriteProductIds.contains(product.getId());

            return ProductResponseDTO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                    .lowestPrice(lowestPrice)
                    .brand(product.getBrand())
                    .isFlashSale(isFlashSale)
                    .discountedPrice(discountedPrice)
                    .originalPrice(originalPrice)
                    .discountOverrideByFlashSale(discountOverrideByFlashSale)
                    .discountType(discountType)
                    .soldQuantity(soldQuantity)
                    .slug(slug)
                    .isFavorite(isFavorite)
                    .updatedAt(product.getUpdatedAt())
                    .status(product.getStatus())
                    .averageRating(averageRating)
                    .totalReviews(totalReviews)
                    .stockQuantity(totalStock)
                    .variantCount(variantDTOs.size())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0).getImageUrl() : null)
                    .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                    .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                    .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                    .createdAt(product.getCreatedAt())
                    .variants(variantDTOs)
                    .build();
        }).collect(Collectors.toList());
    }

    private String generateSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // remove accents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")     // Remove special characters
                .replaceAll("\\s+", "-")             // Replace spaces with hyphens
                .replaceAll("-+", "-")               // Merge multiple hyphens
                .replaceAll("^-|-$", "");            // Remove hyphen at the ends
        // Ensure uniqueness by appending a number if needed
        String baseSlug = slug;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    @Override
    public FlashSale save(FlashSale flashSale) {
        return flashSaleRepository.save(flashSale);
    }
}