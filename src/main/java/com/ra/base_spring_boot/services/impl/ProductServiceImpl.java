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
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ra.base_spring_boot.security.principle.MyUserDetails;

import java.math.RoundingMode;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final IProductRepository productRepository;

    private final ICategoryRepository categoryRepository;

    private final IOrderItemRepository orderItemRepository;

    private final IReturnPolicyRepository returnPolicyRepository;

    private final IWishListRepository wishListRepository;

    @Autowired
    private IColorRepository colorRepository;
    @Autowired
    private ISizeRepository sizeRepository;
    @Autowired
    private IFlashSaleItemRepository flashSaleItemRepository;
    @Autowired
    private IFlashSaleRepository flashSaleRepository;
    @Autowired
    private IReviewRepository reviewRepository;

    private static final Logger log = LoggerFactory.getLogger(FlashSaleServiceImpl.class);

    @Override
    public List<ProductResponseDTO> findAll() {
        // Fetch active flash sale using the repository method
        LocalDateTime now = LocalDateTime.now();
        Optional<FlashSale> activeFlashSaleOptional = flashSaleRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now);
        List<FlashSaleItem> activeFlashSaleItems = activeFlashSaleOptional.map(flashSale ->
                flashSaleItemRepository.findByFlashSaleId(flashSale.getId())
        ).orElse(List.of());

        // Map flash sale items to variant IDs
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Fetch all products
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product -> {
                    // Fetch order items for all variants of this product in one query
                    List<Long> variantIds = product.getVariants() != null
                            ? product.getVariants().stream()
                            .map(ProductVariant::getId)
                            .toList()
                            : List.of();
                    List<OrderItem> orderItems = variantIds.isEmpty()
                            ? List.of()
                            : orderItemRepository.findByVariantIdIn(variantIds);
                    Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                            .collect(Collectors.groupingBy(
                                    item -> item.getVariant().getId(),
                                    Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                            ));

                    // Map variants to DTOs
                    List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                            ? product.getVariants().stream().map(variant -> {
                        BigDecimal priceOriginal = variant.getPriceOverride() != null ? variant.getPriceOverride() : BigDecimal.ZERO;
                        BigDecimal finalPrice = priceOriginal;
                        BigDecimal discountOverride = BigDecimal.ZERO;
                        String discountType = null;

                        // Apply flash sale discounts
                        if (flashSaleItemMap.containsKey(variant.getId())) {
                            FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                            discountOverride = item.getDiscountedPrice() != null ? item.getDiscountedPrice() : BigDecimal.ZERO;
                            discountType = item.getDiscountType() != null ? item.getDiscountType().name() : null;

                            if (item.getDiscountType() == DiscountType.PERCENTAGE && item.getDiscountedPrice() != null) {
                                BigDecimal percent = item.getDiscountedPrice();
                                BigDecimal discountAmount = priceOriginal.multiply(percent)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                finalPrice = priceOriginal.subtract(discountAmount);
                            } else if (item.getDiscountType() == DiscountType.AMOUNT && item.getDiscountedPrice() != null) {
                                finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                            }
                        }

                        // Get sold quantity for this variant
                        int soldQuantity = variantSoldQuantities.getOrDefault(variant.getId(), 0);

                        return ProductVariantResponseDTO.builder()
                                .id(variant.getId())
                                .productName(product.getName())
                                .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                                .stockQuantity(variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                                .priceOverride(priceOriginal)
                                .discountOverrideByFlashSale(finalPrice)
                                .discountType(discountType)
                                .finalPriceAfterDiscount(finalPrice)
                                .soldQuantity(soldQuantity)
                                .build();
                    }).toList()
                            : List.of();

                    // Calculate total stock and lowest price
                    int totalStock = variantDTOs.stream()
                            .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                            .sum();

                    BigDecimal lowestPrice = variantDTOs.stream()
                            .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

                    // Check if product is part of a flash sale
                    boolean isFlashSale = !variantDTOs.isEmpty() && variantDTOs.stream()
                            .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

                    // Aggregate discounted price and discount details for the product
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

                    // Fetch reviews
                    List<Review> reviews = reviewRepository.findAllByProduct(product);
                    long totalReviews = reviews.size();
                    double averageRating = reviews.stream()
                            .mapToDouble(Review::getRating)
                            .average()
                            .orElse(0.0);

                    // Calculate total sold quantity for the product
                    int soldQuantity = variantDTOs.stream()
                            .mapToInt(dto -> dto.getSoldQuantity() != null ? dto.getSoldQuantity() : 0)
                            .sum();

                    // Derive slug and isFavorite
                    String slug = product.getSlug() != null ? product.getSlug() : generateSlug(product.getName());
                    Boolean isFavorite = false; // Placeholder: Implement user-specific logic if needed

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
                                    ? product.getImages().get(0).getImageUrl()
                                    : null)
                            .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                            .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                            .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                            .createdAt(product.getCreatedAt())
                            .variants(variantDTOs)
                            .build();
                }).toList();
    }

    @Override
    public Page<ProductResponseDTO> getProductsPaginate(
            String keyword,
            Long categoryId,
            String categorySlug,
            String status,
            String brandName,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Integer minRating,
            int page,
            int limit,
            String sortBy,
            String orderBy
    ) {
        // Ensure zero-based pagination
        Pageable pageable = PageRequest.of(
                page,
                limit,
                orderBy.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null && !sortBy.isEmpty() ? sortBy : "createdAt"
        );

        // Build Specification for filtering
        Specification<Product> spec = Specification.where(null);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), likePattern),
                            cb.like(cb.lower(root.get("brand")), likePattern),
                            cb.like(cb.lower(root.get("description")), likePattern)
                    )
            );
        }

        if (categoryId != null || (categorySlug != null && !categorySlug.trim().isEmpty())) {
            Long resolvedCategoryId = categoryId;
            if (resolvedCategoryId == null && categorySlug != null) {
                Optional<Category> category = categoryRepository.findBySlug(categorySlug);
                if (category.isPresent()) {
                    resolvedCategoryId = category.get().getId();
                } else {
                    log.warn("Invalid category slug: {}", categorySlug);
                    return Page.empty(pageable);
                }
            }
            if (resolvedCategoryId != null) {
                List<Long> categoryIds = getAllChildCategoryIds(resolvedCategoryId);
                spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categoryIds));
            }
        }

        if (status != null && !status.trim().isEmpty()) {
            try {
                ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), productStatus));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid product status: {}", status);
            }
        }

        if (brandName != null && !brandName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brandName.toLowerCase()));
        }

        if (priceMin != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<BigDecimal> priceSubquery = query.subquery(BigDecimal.class);
                Root<ProductVariant> variantRoot = priceSubquery.from(ProductVariant.class);
                priceSubquery.select(cb.coalesce(cb.min(variantRoot.get("priceOverride")), root.get("price")))
                        .where(cb.equal(variantRoot.get("product").get("id"), root.get("id")));
                return cb.greaterThanOrEqualTo(priceSubquery, priceMin);
            });
        }

        if (priceMax != null) {
            spec = spec.and((root, query, cb) -> {
                Subquery<BigDecimal> priceSubquery = query.subquery(BigDecimal.class);
                Root<ProductVariant> variantRoot = priceSubquery.from(ProductVariant.class);
                priceSubquery.select(cb.coalesce(cb.min(variantRoot.get("priceOverride")), root.get("price")))
                        .where(cb.equal(variantRoot.get("product").get("id"), root.get("id")));
                return cb.lessThanOrEqualTo(priceSubquery, priceMax);
            });
        }

        if (minRating != null && minRating > 0) {
            spec = spec.and((root, query, cb) -> {
                Subquery<Double> ratingSubquery = query.subquery(Double.class);
                Root<Review> reviewRoot = ratingSubquery.from(Review.class);
                ratingSubquery.select(cb.avg(reviewRoot.get("rating")))
                        .where(cb.equal(reviewRoot.get("product").get("id"), root.get("id")));
                return cb.greaterThanOrEqualTo(ratingSubquery, (double) minRating);
            });
        }

        // Fetch paginated products
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        if (productPage.isEmpty()) {
            log.info("No products found for the given filters");
            return Page.empty(pageable);
        }

        // Get logged-in user's ID from SecurityContextHolder
        final Long userId = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal())
                .filter(principal -> principal instanceof MyUserDetails)
                .map(principal -> ((MyUserDetails) principal).getUser().getId())
                .orElse(null);

        // Fetch wishlist entries for the current page's products
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();
        List<Wishlist> wishlists = (userId != null && !productIds.isEmpty())
                ? wishListRepository.findAllByUser_IdAndProduct_IdIn(userId, productIds)
                : List.of();
        Set<Long> favoriteProductIds = wishlists.stream()
                .map(wishlist -> wishlist.getProduct().getId())
                .collect(Collectors.toSet());

        // Fetch active flash sale using the repository method
        LocalDateTime now = LocalDateTime.now();
        Optional<FlashSale> activeFlashSaleOptional = flashSaleRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now);
        List<FlashSaleItem> activeFlashSaleItems = activeFlashSaleOptional.map(flashSale ->
                flashSaleItemRepository.findByFlashSaleId(flashSale.getId())
        ).orElse(List.of());

        // Map flash sale items to a map for quick lookup
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(
                        item -> item.getVariant().getId(),
                        item -> item,
                        (a, b) -> a
                ));

        // Fetch order items for variant-level sold quantities
        List<Long> allVariantIds = productPage.getContent().stream()
                .filter(product -> product.getVariants() != null)
                .flatMap(product -> product.getVariants().stream())
                .map(ProductVariant::getId)
                .toList();
        List<OrderItem> orderItems = allVariantIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByVariantIdIn(allVariantIds);
        Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // Map products to DTOs
        return productPage.map(product -> {
            // Fetch sold quantity for the product
            Integer soldQuantity = orderItemRepository.countSoldQuantityByProductId(product.getId());
            if (soldQuantity == null) {
                soldQuantity = 0;
            }

            // Map variants to DTOs
            List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                    ? product.getVariants().stream().map(variant -> {
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
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        finalPrice = priceOriginal.subtract(discountAmount);
                    } else if (item.getDiscountType() == DiscountType.AMOUNT && item.getDiscountedPrice() != null) {
                        finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                    }
                }

                int variantSoldQuantity = variantSoldQuantities.getOrDefault(variant.getId(), 0);

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
                        .soldQuantity(variantSoldQuantity)
                        .finalPriceAfterDiscount(finalPrice)
                        .build();
            }).toList()
                    : List.of();

            // Calculate total stock
            int totalStock = variantDTOs.stream()
                    .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                    .sum();

            // Calculate reviews
            List<Review> reviews = reviewRepository.findAllByProduct(product);
            long totalReviews = reviews.size();
            double averageRating = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);

            // Check if product is part of a flash sale
            boolean isFlashSale = !variantDTOs.isEmpty() && variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Calculate lowest price
            BigDecimal lowestPrice = variantDTOs.stream()
                    .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

            // Calculate discountedPrice, originalPrice, discountOverrideByFlashSale
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

            // Check if product is in user's wishlist
            Boolean isFavorite = userId != null && favoriteProductIds.contains(product.getId());

            // Derive slug
            String productSlug = product.getSlug() != null ? product.getSlug() : generateSlug(product.getName());

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
                    .slug(productSlug)
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
        });
    }

    public List<Long> getAllChildCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId); // bản thân nó

        List<Category> children = categoryRepository.findByParentId(categoryId);
        for (Category child : children) {
            ids.addAll(getAllChildCategoryIds(child.getId()));
        }

        return ids;
    }

    @Override
    public ProductResponseDTO findById(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return null;
        }

        // Get logged-in user's ID from SecurityContextHolder
        final Long userId = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal())
                .filter(principal -> principal instanceof MyUserDetails)
                .map(principal -> ((MyUserDetails) principal).getUser().getId())
                .orElse(null);

        // Check if product is in user's wishlist
        boolean isFavorite = false;
        if (userId != null) {
            List<Wishlist> wishlists = wishListRepository.findAllByUser_IdAndProduct_IdIn(userId, List.of(product.getId()));
            isFavorite = !wishlists.isEmpty();
        }

        // Fetch active flash sales
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> activeFlashSales = flashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(now)
                        && flashSale.getEndTime().isAfter(now))
                .toList();
        List<FlashSaleItem> activeFlashSaleItems = activeFlashSales.stream()
                .flatMap(flashSale -> flashSaleItemRepository.findByFlashSaleId(flashSale.getId()).stream())
                .toList();

        // Map variantId -> FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Fetch sold quantity for the product
        Integer soldQuantity = orderItemRepository.countSoldQuantityByProductId(product.getId());
        if (soldQuantity == null) {
            soldQuantity = 0;
        }

        // Fetch order items for variant-level sold quantities
        List<Long> variantIds = product.getVariants() != null
                ? product.getVariants().stream().map(ProductVariant::getId).toList()
                : List.of();
        List<OrderItem> orderItems = variantIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByVariantIdIn(variantIds);
        Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // Map variants to DTOs
        List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                ? product.getVariants().stream().map(variant -> {
            BigDecimal priceOriginal = variant.getPriceOverride();
            BigDecimal finalPrice = priceOriginal;
            BigDecimal discountOverride = BigDecimal.ZERO;
            String discountType = null;

            if (flashSaleItemMap.containsKey(variant.getId())) {
                FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                discountOverride = item.getDiscountedPrice();
                discountType = item.getDiscountType().name();

                if (item.getDiscountType() == DiscountType.PERCENTAGE) {
                    BigDecimal percent = item.getDiscountedPrice();
                    BigDecimal discountAmount = priceOriginal.multiply(percent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    finalPrice = priceOriginal.subtract(discountAmount);
                } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                    finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                }
            }

            int variantSoldQuantity = variantSoldQuantities.getOrDefault(variant.getId(), 0);

            return ProductVariantResponseDTO.builder()
                    .id(variant.getId())
                    .productName(product.getName())
                    .colorId(variant.getColor() != null ? variant.getColor().getId() : null)
                    .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                    .sizeId(variant.getSize() != null ? variant.getSize().getId() : null)
                    .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                    .stockQuantity(variant.getStockQuantity())
                    .priceOverride(priceOriginal)
                    .discountOverrideByFlashSale(discountOverride)
                    .discountType(discountType)
                    .finalPriceAfterDiscount(finalPrice)
                    .soldQuantity(variantSoldQuantity)
                    .sku(variant.getSku())
                    .barcode(variant.getBarcode())
                    .build();
        }).toList()
                : List.of();

        // Calculate total stock
        int totalStock = variantDTOs.stream()
                .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                .sum();

        // Calculate average rating and total reviews
        List<Review> reviews = reviewRepository.findAllByProduct(product);
        long totalReviews = reviews.size();
        double averageRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        // Check if any variant is in a flash sale
        boolean isFlashSale = variantDTOs.stream()
                .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

        // Calculate lowest price and discounted price
        BigDecimal lowestPrice = variantDTOs.stream()
                .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

        BigDecimal discountedPrice = isFlashSale
                ? variantDTOs.stream()
                .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(lowestPrice)
                : null;

        BigDecimal discountOverrideByFlashSale = isFlashSale
                ? variantDTOs.stream()
                .map(ProductVariantResponseDTO::getDiscountOverrideByFlashSale)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                : null;

        String discountType = isFlashSale
                ? variantDTOs.stream()
                .filter(dto -> dto.getDiscountType() != null)
                .map(ProductVariantResponseDTO::getDiscountType)
                .findFirst()
                .orElse(null)
                : null;

        // Derive slug
        String slug = product.getSlug() != null ? product.getSlug() : generateSlug(product.getName());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .lowestPrice(lowestPrice)
                .brand(product.getBrand())
                .isFlashSale(isFlashSale)
                .discountedPrice(discountedPrice)
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
                .build();
    }

    @Override
    public ProductResponseDTO findBySlug(String slug) {
        Product product = productRepository.findBySlug(slug).orElse(null);
        if (product == null) {
            return null;
        }

        // Get logged-in user's ID from SecurityContextHolder
        final Long userId = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal())
                .filter(principal -> principal instanceof MyUserDetails)
                .map(principal -> ((MyUserDetails) principal).getUser().getId())
                .orElse(null);

        // Check if product is in user's wishlist
        boolean isFavorite = false;
        if (userId != null) {
            List<Wishlist> wishlists = wishListRepository.findAllByUser_IdAndProduct_IdIn(userId, List.of(product.getId()));
            isFavorite = !wishlists.isEmpty();
        }

        // Fetch active flash sales
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> activeFlashSales = flashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(now)
                        && flashSale.getEndTime().isAfter(now))
                .toList();
        List<FlashSaleItem> activeFlashSaleItems = activeFlashSales.stream()
                .flatMap(flashSale -> flashSaleItemRepository.findByFlashSaleId(flashSale.getId()).stream())
                .toList();

        // Map variantId -> FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Fetch sold quantity for the product
        Integer soldQuantity = orderItemRepository.countSoldQuantityByProductId(product.getId());
        if (soldQuantity == null) {
            soldQuantity = 0;
        }

        // Fetch order items for variant-level sold quantities
        List<Long> variantIds = product.getVariants() != null
                ? product.getVariants().stream().map(ProductVariant::getId).toList()
                : List.of();
        List<OrderItem> orderItems = variantIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByVariantIdIn(variantIds);
        Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // Map variants to DTOs
        List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                ? product.getVariants().stream().map(variant -> {
            BigDecimal priceOriginal = variant.getPriceOverride();
            BigDecimal finalPrice = priceOriginal;
            BigDecimal discountOverride = BigDecimal.ZERO;
            String discountType = null;

            if (flashSaleItemMap.containsKey(variant.getId())) {
                FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                discountOverride = item.getDiscountedPrice();
                discountType = item.getDiscountType().name();

                if (item.getDiscountType() == DiscountType.PERCENTAGE) {
                    BigDecimal percent = item.getDiscountedPrice();
                    BigDecimal discountAmount = priceOriginal.multiply(percent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    finalPrice = priceOriginal.subtract(discountAmount);
                } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                    finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                }
            }

            int variantSoldQuantity = variantSoldQuantities.getOrDefault(variant.getId(), 0);

            return ProductVariantResponseDTO.builder()
                    .id(variant.getId())
                    .productName(product.getName())
                    .colorId(variant.getColor() != null ? variant.getColor().getId() : null)
                    .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                    .sizeId(variant.getSize() != null ? variant.getSize().getId() : null)
                    .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                    .stockQuantity(variant.getStockQuantity())
                    .priceOverride(priceOriginal)
                    .discountOverrideByFlashSale(discountOverride)
                    .discountType(discountType)
                    .finalPriceAfterDiscount(finalPrice)
                    .soldQuantity(variantSoldQuantity)
                    .sku(variant.getSku())
                    .barcode(variant.getBarcode())
                    .build();
        }).toList()
                : List.of();

        // Calculate total stock
        int totalStock = variantDTOs.stream()
                .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                .sum();

        // Calculate average rating and total reviews
        List<Review> reviews = reviewRepository.findAllByProduct(product);
        long totalReviews = reviews.size();
        double averageRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        // Check if any variant is in a flash sale
        boolean isFlashSale = variantDTOs.stream()
                .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

        // Calculate lowest price and discounted price
        BigDecimal lowestPrice = variantDTOs.stream()
                .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

        BigDecimal discountedPrice = isFlashSale
                ? variantDTOs.stream()
                .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(lowestPrice)
                : null;

        BigDecimal discountOverrideByFlashSale = isFlashSale
                ? variantDTOs.stream()
                .map(ProductVariantResponseDTO::getDiscountOverrideByFlashSale)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                : null;

        String discountType = isFlashSale
                ? variantDTOs.stream()
                .filter(dto -> dto.getDiscountType() != null)
                .map(ProductVariantResponseDTO::getDiscountType)
                .findFirst()
                .orElse(null)
                : null;

        // Derive slug
        String productSlug = product.getSlug() != null ? product.getSlug() : generateSlug(product.getName());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .lowestPrice(lowestPrice)
                .brand(product.getBrand())
                .isFlashSale(isFlashSale)
                .discountedPrice(discountedPrice)
                .discountOverrideByFlashSale(discountOverrideByFlashSale)
                .discountType(discountType)
                .soldQuantity(soldQuantity)
                .slug(productSlug)
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
                .build();
    }

    @Override
    public ProductResponseDTO save(ProductRequestDTO dto) {
        // Validate category
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new HttpNotFound("Danh mục không tồn tại"));

        // Validate return policy
        ReturnPolicy returnPolicy = returnPolicyRepository.findById(dto.getReturn_policy_id())
                .orElseThrow(() -> new HttpNotFound("Chính sách trả hàng không tồn tại"));

        // Generate slug if not provided or invalid
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank() ? dto.getSlug() : generateSlug(dto.getName());

        // Check for unique slug
        if (productRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug đã tồn tại");
        }

        // Optional: Check for unique name if enforced
        // if (productRepository.existsByNameIgnoreCase(dto.getName())) {
        //     throw new IllegalArgumentException("Tên sản phẩm đã tồn tại");
        // }

        // Build product entity
        LocalDateTime now = LocalDateTime.now();
        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .brand(dto.getBrand())
                .slug(slug)
                .status(dto.getStatus())
                .category(category)
                .returnPolicy(returnPolicy)
                .createdAt(now)
                .updatedAt(now)     // Set updatedAt to creation time initially
                .deleted(false)
                .build();

        // Save product to database
        product = productRepository.save(product);

        // Build and return response DTO
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .slug(product.getSlug())
                .status(product.getStatus())
                .categoryName(product.getCategory().getName())
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl()
                        : null)
                .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
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
        existing.setUpdatedAt(LocalDateTime.now());

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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Product not found with id: " + id));
        product.setDeleted(true);
        productRepository.save(product);
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

    @Override
    public List<ProductResponseDTO> getTop5BestSellingProducts() {
        // Fetch top 5 best-selling products
        List<Object[]> result = productRepository.findTop5BestSellingProducts(
                OrderStatus.DELIVERED,
                PageRequest.of(0, 5)
        );

        // Get logged-in user's ID from SecurityContextHolder
        final Long userId = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getPrincipal())
                .filter(principal -> principal instanceof MyUserDetails)
                .map(principal -> ((MyUserDetails) principal).getUser().getId())
                .orElse(null);

        // Fetch product IDs for wishlist check
        List<Long> productIds = result.stream()
                .map(row -> ((Product) row[0]).getId())
                .toList();

        // Fetch wishlist entries for the products
        List<Wishlist> wishlists = (userId != null && !productIds.isEmpty())
                ? wishListRepository.findAllByUser_IdAndProduct_IdIn(userId, productIds)
                : List.of();
        Set<Long> favoriteProductIds = wishlists.stream()
                .map(wishlist -> wishlist.getProduct().getId())
                .collect(Collectors.toSet());

        // Fetch active flash sale using the repository method
        LocalDateTime now = LocalDateTime.now();
        Optional<FlashSale> activeFlashSaleOptional = flashSaleRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now);
        List<FlashSaleItem> activeFlashSaleItems = activeFlashSaleOptional.map(flashSale ->
                flashSaleItemRepository.findByFlashSaleId(flashSale.getId())
        ).orElse(List.of());

        // Map variantId -> FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Fetch order items for variant-level sold quantities
        List<Long> allVariantIds = result.stream()
                .map(row -> (Product) row[0])
                .filter(product -> product.getVariants() != null)
                .flatMap(product -> product.getVariants().stream())
                .map(ProductVariant::getId)
                .toList();
        List<OrderItem> orderItems = allVariantIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByVariantIdIn(allVariantIds);
        Map<Long, Integer> variantSoldQuantities = orderItems.stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // Map results to ProductResponseDTO
        return result.stream().map(row -> {
            Product product = (Product) row[0];
            Long purchaseCount = (Long) row[1];
            Double avgRating = (Double) row[2];
            Long totalReviews = (Long) row[3];

            // Map product variants to DTOs
            List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                    ? product.getVariants().stream().map(variant -> {
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
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        finalPrice = priceOriginal.subtract(discountAmount);
                    } else if (item.getDiscountType() == DiscountType.AMOUNT && item.getDiscountedPrice() != null) {
                        finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                    }
                }

                int variantSoldQuantity = variantSoldQuantities.getOrDefault(variant.getId(), 0);

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
                        .soldQuantity(variantSoldQuantity)
                        .finalPriceAfterDiscount(finalPrice)
                        .build();
            }).toList()
                    : List.of();

            // Calculate total stock
            int totalStock = variantDTOs.stream()
                    .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                    .sum();

            // Check if any variant is in a flash sale
            boolean isFlashSale = !variantDTOs.isEmpty() && variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Calculate lowest price
            BigDecimal lowestPrice = variantDTOs.stream()
                    .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

            // Calculate discountedPrice, originalPrice, discountOverrideByFlashSale
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
                    .soldQuantity(purchaseCount != null ? purchaseCount.intValue() : 0)
                    .slug(slug)
                    .isFavorite(isFavorite)
                    .updatedAt(product.getUpdatedAt())
                    .status(product.getStatus())
                    .averageRating(avgRating != null ? avgRating : 0.0)
                    .totalReviews(totalReviews != null ? totalReviews : 0L)
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
        }).toList();
    }

    @Override
    public ProductResponseDTO findByName(String productName) {
        // Tìm kiếm sản phẩm theo tên, không phân biệt hoa thường
        Optional<Product> productOptional = productRepository.findByNameIgnoreCase(productName);

        // Nếu không tìm thấy thì trả về null (hoặc có thể ném exception nếu muốn)
        if (productOptional.isEmpty()) {
            return null;
        }

        Product product = productOptional.get();

        // Convert Entity => DTO
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .status(product.getStatus())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }

    @Override
    public List<ProductResponseDTO> getRelatedProducts(Long productId) {
        Product currentProduct = productRepository.findById(productId)
                .orElseThrow(() -> new HttpNotFound("Product Not Found"));

        List<Product> related = productRepository
                .findTop4ByCategoryAndIdNot(currentProduct.getCategory(), currentProduct.getId());

        if (related.isEmpty()) {
            throw new HttpNotFound("Related Product Not Found");
        }

        // Fetch active flash sales
        List<FlashSale> activeFlashSales = flashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(LocalDateTime.now())
                        && flashSale.getEndTime().isAfter(LocalDateTime.now()))
                .toList();

        // Fetch active flash sale items
        List<FlashSaleItem> activeFlashSaleItems = new ArrayList<>();
        for (FlashSale flashSale : activeFlashSales) {
            activeFlashSaleItems.addAll(flashSaleItemRepository.findByFlashSaleId(flashSale.getId()));
        }

        // Map variantId -> FlashSaleItem
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Process each related product
        List<ProductResponseDTO> relatedProductDTOs = related.stream().map(product -> {
            // Map variants to DTOs
            List<ProductVariantResponseDTO> variantDTOs = product.getVariants() != null
                    ? product.getVariants().stream().map(variant -> {
                BigDecimal priceOriginal = variant.getPriceOverride();
                BigDecimal finalPrice = priceOriginal;
                String discountType = null;
                BigDecimal discountOverrideByFlashSale = null;

                ProductVariantResponseDTO dto = ProductVariantResponseDTO.builder()
                        .id(variant.getId())
                        .productName(product.getName())
                        .colorId(variant.getColor() != null ? variant.getColor().getId() : null)
                        .colorName(variant.getColor() != null ? variant.getColor().getName() : null)
                        .sizeId(variant.getSize() != null ? variant.getSize().getId() : null)
                        .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                        .stockQuantity(variant.getStockQuantity())
                        .priceOverride(priceOriginal)
                        .sku(variant.getSku())
                        .barcode(variant.getBarcode())
                        .build();

                if (flashSaleItemMap.containsKey(variant.getId())) {
                    FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                    discountOverrideByFlashSale = item.getDiscountedPrice();
                    discountType = item.getDiscountType().name();
                    dto.setDiscountOverrideByFlashSale(discountOverrideByFlashSale);
                    dto.setDiscountType(discountType);

                    if (item.getDiscountType() == DiscountType.PERCENTAGE) {
                        BigDecimal percent = item.getDiscountedPrice();
                        BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                        finalPrice = priceOriginal.subtract(discountAmount);
                    } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                        finalPrice = priceOriginal.subtract(item.getDiscountedPrice());
                    }
                }

                dto.setFinalPriceAfterDiscount(finalPrice);
                return dto;
            }).collect(Collectors.toList())
                    : new ArrayList<>();

            // Calculate total stock
            int totalStock = variantDTOs.stream()
                    .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                    .sum();

            // Calculate average rating and total reviews
            List<Review> reviews = reviewRepository.findAllByProduct(product);
            long totalReviews = reviews.size();
            double averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);

            // Check if any variant is in a flash sale
            boolean isFlashSale = variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Calculate lowest price
            BigDecimal lowestPrice = null;
            BigDecimal discountedPrice = null;
            BigDecimal discountOverrideByFlashSale = null;
            String discountType = null;

            if (!variantDTOs.isEmpty()) {
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

                    discountedPrice = variantDTOs.stream()
                            .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(lowestPrice);

                    ProductVariantResponseDTO lowestFlashSaleVariant = variantDTOs.stream()
                            .filter(dto -> flashSaleItemMap.containsKey(dto.getId()))
                            .filter(dto -> dto.getFinalPriceAfterDiscount() != null)
                            .min(Comparator.comparing(ProductVariantResponseDTO::getFinalPriceAfterDiscount))
                            .orElse(null);
                    if (lowestFlashSaleVariant != null) {
                        discountOverrideByFlashSale = lowestFlashSaleVariant.getDiscountOverrideByFlashSale();
                        discountType = lowestFlashSaleVariant.getDiscountType();
                    }
                } else {
                    lowestPrice = variantDTOs.stream()
                            .map(ProductVariantResponseDTO::getPriceOverride)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(product.getPrice());
                    discountedPrice = lowestPrice;
                }
            } else {
                lowestPrice = product.getPrice();
                discountedPrice = product.getPrice();
            }

            return ProductResponseDTO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .brand(product.getBrand())
                    .status(product.getStatus())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0).getImageUrl() : null)
                    .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                    .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                    .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                    .createdAt(product.getCreatedAt())
                    .stockQuantity(totalStock)
                    .averageRating(averageRating)
                    .totalReviews(totalReviews)
                    .variants(variantDTOs)
                    .isFlashSale(isFlashSale)
                    .lowestPrice(lowestPrice)
                    .discountedPrice(discountedPrice)
                    .discountOverrideByFlashSale(discountOverrideByFlashSale)
                    .discountType(discountType)
                    .build();
        }).collect(Collectors.toList());

        return relatedProductDTOs;
    }


}