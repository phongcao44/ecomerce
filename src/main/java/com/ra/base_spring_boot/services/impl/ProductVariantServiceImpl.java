package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.ProductVariantRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantDetailDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.DiscountType;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.services.IProductService;
import com.ra.base_spring_boot.services.IProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private IFlashSaleItemRepository  flashSaleItemRepository;
    @Autowired
    private IFlashSaleRepository flashSaleRepository;

    @Override
    public List<ProductVariantResponseDTO> findAll() {
        // B1: Lấy danh sách các Flash Sale đang hoạt động
        List<FlashSale> activeFlashSales = flashSaleRepository.findAll().stream()
                .filter(flashSale -> flashSale.getStatus() == UserStatus.ACTIVE
                        && flashSale.getStartTime().isBefore(LocalDateTime.now())
                        && flashSale.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        // B2: Lấy tất cả các item thuộc các Flash Sale đang hoạt động
        List<FlashSaleItem> activeFlashSaleItems = new ArrayList<>();
        for (FlashSale flashSale : activeFlashSales) {
            List<FlashSaleItem> items = flashSaleItemRepository.findByFlashSaleId(flashSale.getId());
            activeFlashSaleItems.addAll(items);
        }

        // B3: Ánh xạ variantId -> FlashSaleItem để tra nhanh
        Map<Long, FlashSaleItem> flashSaleItemMap = activeFlashSaleItems.stream()
                .filter(item -> item.getVariant() != null)
                .collect(Collectors.toMap(item -> item.getVariant().getId(), item -> item, (a, b) -> a));

        // B4: Tạo danh sách DTO và tính giá giảm nếu có Flash Sale
        List<ProductVariant> variants = productVariantRepository.findAll();
        return variants.stream().map(variant -> {
            BigDecimal priceOriginal = variant.getPriceOverride();
            BigDecimal finalPrice = priceOriginal;
            ProductVariantResponseDTO dto = ProductVariantResponseDTO.builder()
                    .id(variant.getId())
                    .productName(variant.getProduct().getName())
                    .colorName(variant.getColor() !=null ? variant.getColor().getName():null)
                    .sizeName(variant.getSize() !=null ? variant.getSize().getSizeName():null)
                    .stockQuantity(variant.getStockQuantity())
                    .priceOverride(variant.getPriceOverride())
                    .build();

            // Nếu variant đang có Flash Sale thì gán giá giảm
            if (flashSaleItemMap.containsKey(variant.getId())) {
                FlashSaleItem item = flashSaleItemMap.get(variant.getId());
                dto.setDiscountOverrideByFlashSale(item.getDiscountedPrice());
                dto.setDiscountType(item.getDiscountType().name());
                // Tính giá sau giảm
                if (item.getDiscountType() == DiscountType.PERCENTAGE) {
                    BigDecimal percent = item.getDiscountedPrice(); // ví dụ: 10%
                    BigDecimal discountAmount = priceOriginal.multiply(percent).divide(BigDecimal.valueOf(100));
                    finalPrice = priceOriginal.subtract(discountAmount);
                } else if (item.getDiscountType() == DiscountType.AMOUNT) {
                    BigDecimal discountAmount = item.getDiscountedPrice();
                    finalPrice = priceOriginal.subtract(discountAmount);
                }
            }
            dto.setFinalPriceAfterDiscount(finalPrice);
            return dto;
        }).collect(Collectors.toList());
    }



    @Override
    public List<ProductVariantResponseDTO> findByProductId(Long productId) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        return variants.stream().map(variant -> ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .productName(variant.getProduct().getName())
                .colorName(variant.getColor() !=null ? variant.getColor().getName() : null)
                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                .stockQuantity(variant.getStockQuantity())
                .priceOverride(variant.getPriceOverride())
                .build()).collect(Collectors.toList());
    }


    @Override
    public ProductVariantResponseDTO create(ProductVariantRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new HttpNotFound("ProductId Not Found"));

        Long colorId = (dto.getColorId() != null && dto.getColorId() == 0L) ? null : dto.getColorId();
        Long sizeId = (dto.getSizeId() != null && dto.getSizeId() == 0L) ? null : dto.getSizeId();

        Color color = null;
        if (colorId != null) {
            color = colorRepository.findById(colorId)
                    .orElseThrow(() -> new HttpNotFound("ColorId Not Found"));
        }

        Size size = null;
        if (sizeId != null) {
            size = sizeRepository.findById(sizeId)
                    .orElseThrow(() -> new HttpNotFound("SizeId Not Found"));
        }

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
                .colorName(color !=null ? color.getName() : null)
                .sizeName(size !=null ? size.getSizeName() : null)
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

        Long colorId = (dto.getColorId() != null && dto.getColorId() == 0L) ? null : dto.getColorId();
        Long sizeId = (dto.getSizeId() != null && dto.getSizeId() == 0L) ? null : dto.getSizeId();

        Color color = null;
        if (colorId != null) {
            color = colorRepository.findById(colorId)
                    .orElseThrow(() -> new HttpNotFound("ColorId Not Found"));
        }

        Size size = null;
        if (sizeId != null) {
            size = sizeRepository.findById(sizeId)
                    .orElseThrow(() -> new HttpNotFound("SizeId Not Found"));
        }

        variant.setProduct(product);
        variant.setColor(color);
        variant.setSize(size);
        variant.setStockQuantity(dto.getStockQuantity());
        variant.setPriceOverride(dto.getPriceOverride());

        variant = productVariantRepository.save(variant);

        return ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .productName(product.getName())
                .colorName(color !=null ? color.getName() : null)
                .sizeName(size !=null ? size.getSizeName() : null)
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
                .colorName(color !=null ? color.getName() : null)
                .colorHex(color !=null ? color.getHexCode() : null)
                .sizeName(size !=null ? size.getSizeName() : null)
                .sizeDescription(size !=null ? size.getDescription() : null)
                .build();
    }

    @Override
    public List<ProductVariantDetailDTO> findAllVariantDetails() {
        return productVariantRepository.findAll().stream().map(variant -> {
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
                    .colorName(color != null ? color.getName() : null)
                    .colorHex(color != null ? color.getHexCode() : null)
                    .sizeName(size != null ? size.getSizeName() : null)
                    .sizeDescription(size != null ? size.getDescription() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProductVariantDetailDTO> findAllVariantsByProductName(String productName) {
        List<ProductVariant> variants = productVariantRepository.findByProduct_NameIgnoreCase(productName);

        // Chuyển đổi danh sách ProductVariant Entity sang danh sách ProductVariantDetailDTO
        return variants.stream()
                .map(this::mapVariantToDetailDTO)
                .collect(Collectors.toList());
    }

    private ProductVariantDetailDTO mapVariantToDetailDTO(ProductVariant variant) {
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
                .colorName(color != null ? color.getName() : null)
                .colorHex(color != null ? color.getHexCode() : null)
                .sizeName(size != null ? size.getSizeName() : null)
                .sizeDescription(size != null ? size.getDescription() : null)
                .build();
    }

}

