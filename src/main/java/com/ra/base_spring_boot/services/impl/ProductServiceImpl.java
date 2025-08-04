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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private IFlashSaleItemRepository flashSaleItemRepository;
    @Autowired
    private IFlashSaleRepository flashSaleRepository;
    @Autowired
    private IReviewRepository reviewRepository;


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

                    List<Review> reviews = reviewRepository.findAllByProduct(product);
                    long totalReviews = reviews.size();
                    double averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);

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
                            .averageRating(averageRating)
                            .totalReviews(totalReviews) // dùng lại field này cho view nếu không tách riêng
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponseDTO> getProductsPaginate(
            String keyword,
            Long categoryId,
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
        // Ensure zero-based pagination (page is already 0-based from frontend)
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

        if (categoryId != null) {
            List<Long> categoryIds = getAllChildCategoryIds(categoryId);
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categoryIds));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (brandName != null && !brandName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brandName.toLowerCase()));
        }

        if (priceMin != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), priceMin));
        }

        if (priceMax != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), priceMax));
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

        // Fetch active flash sale at current time
        LocalDateTime now = LocalDateTime.now(); // Current time
        Optional<FlashSale> activeFlashSale = flashSaleRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now);

        List<FlashSaleItem> activeFlashSaleItems = new ArrayList<>();
        if (activeFlashSale.isPresent()) {
            // Fetch flash sale items for the active flash sale
            activeFlashSaleItems = flashSaleItemRepository.findByFlashSaleId(activeFlashSale.get().getId());
        }

        // Map flash sale items to a map for quick lookup
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // Map products to DTOs
        Page<ProductResponseDTO> dtoPage = productPage.map(product -> {
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
                        BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
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

            // Calculate average rating and total reviews
            List<Review> reviews = reviewRepository.findAllByProduct(product);
            long totalReviews = reviews.size();
            double averageRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);

            // Check if any variant is in a flash sale
            boolean isFlashSale = variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Calculate lowest price, prioritizing flash sale prices
            BigDecimal lowestPrice;
            if (isFlashSale) {
                lowestPrice = variantDTOs.stream()
                        .filter(dto -> flashSaleItemMap.containsKey(dto.getId())) // Only consider variants in flash sale
                        .map(ProductVariantResponseDTO::getFinalPriceAfterDiscount)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElseGet(() -> variantDTOs.stream() // Fallback to non-flash sale prices if needed
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
                    .orElse(lowestPrice); // Fallback to lowestPrice if no discounts

            // Calculate discountOverrideByFlashSale and discountType for the variant with the lowest flash sale price
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
                    .price(product.getPrice())
                    .brand(product.getBrand())
                    .status(product.getStatus())
                    .stockQuantity(totalStock)
                    .variantCount(variantDTOs.size())
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
        });

        return dtoPage;
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
        if (product == null) return null;

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
                    .sku(variant.getSku()) // Thêm sku
                    .barcode(variant.getBarcode()) // Thêm barcode
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

        // Calculate lowest price, prioritizing flash sale prices
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
                .stockQuantity(totalStock)
                .variantCount(variantDTOs.size())
                .variants(variantDTOs)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl() : null)
                .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                .createdAt(product.getCreatedAt())
                .averageRating(reviews.isEmpty() ? null : averageRating)
                .totalReviews(reviews.isEmpty() ? null : totalReviews)
                .lowestPrice(lowestPrice)
                .discountedPrice(discountedPrice)
                .isFlashSale(isFlashSale)
                .discountOverrideByFlashSale(discountOverrideByFlashSale)
                .discountType(discountType)
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
                .createdAt(LocalDateTime.now())
                .deleted(false)
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Product not found with id: " + id));
        product.setDeleted(true); // đánh dấu xóa mềm
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

        // Fetch active flash sales
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

        // Map results to ProductResponseDTO
        return result.stream().map(row -> {
            Product product = (Product) row[0];
            Long purchaseCount = (Long) row[1];
            Double avgRating = (Double) row[2];
            Long totalReviews = (Long) row[3];

            // Map product variants to DTOs
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
                        BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
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

            // Calculate total stock
            int totalStock = variantDTOs.stream()
                    .mapToInt(dto -> dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                    .sum();

            // Check if any variant is in a flash sale
            boolean isFlashSale = variantDTOs.stream()
                    .anyMatch(dto -> flashSaleItemMap.containsKey(dto.getId()));

            // Calculate lowest price, prioritizing flash sale prices
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

            // Calculate discountOverrideByFlashSale and discountType for the variant with the lowest flash sale price
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
                    .price(product.getPrice())
                    .brand(product.getBrand())
                    .status(product.getStatus())
                    .stockQuantity(totalStock)
                    .variantCount(variantDTOs.size())
                    .variants(variantDTOs)
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0).getImageUrl() : null)
                    .returnPolicyId(product.getReturnPolicy() != null ? product.getReturnPolicy().getId() : null)
                    .returnPolicyTitle(product.getReturnPolicy() != null ? product.getReturnPolicy().getTitle() : null)
                    .returnPolicyContent(product.getReturnPolicy() != null ? product.getReturnPolicy().getContent() : null)
                    .createdAt(product.getCreatedAt())
                    .averageRating(avgRating)
                    .totalReviews(totalReviews)
                    .lowestPrice(lowestPrice)
                    .discountedPrice(discountedPrice)
                    .isFlashSale(isFlashSale)
                    .discountOverrideByFlashSale(discountOverrideByFlashSale)
                    .discountType(discountType)
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