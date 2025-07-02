package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.InventoryResponseDTO;
import com.ra.base_spring_boot.dto.resp.InventoryUpdateRequestDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import com.ra.base_spring_boot.services.IInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements IInventoryService {

    private final IProductVariantRepository variantRepository;

    @Override
    public List<InventoryResponseDTO> getAllInventory() {
        return variantRepository.findAll().stream()
                .map(variant -> InventoryResponseDTO.builder()
                        .variantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .color(variant.getColor() != null ? variant.getColor().getName() : null)
                        .size(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                        .quantity(variant.getStockQuantity())
                        .price(variant.getPriceOverride())
                        .build())
                .collect(Collectors.toList());
    }


    @Override
    public void updateStock(InventoryUpdateRequestDTO dto) {
        ProductVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new HttpNotFound("Không tìm thấy sản phẩm biến thể có ID: " + dto.getVariantId()));

        if (dto.getAddedQuantity() == null || dto.getAddedQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }

        variant.setStockQuantity(variant.getStockQuantity() + dto.getAddedQuantity());
        variantRepository.save(variant);

        InventoryResponseDTO.builder()
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .color(variant.getColor() != null ? variant.getColor().getName() : null)
                .size(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                .quantity(variant.getStockQuantity())
                .price(variant.getPriceOverride())
                .build();

    }

    @Override
    public InventoryResponseDTO getByVariantId(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new HttpNotFound("Không tìm thấy sản phẩm biến thể có ID: " + variantId));

        return InventoryResponseDTO.builder()
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .color(variant.getColor() != null ? variant.getColor().getName() : null)
                .size(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                .quantity(variant.getStockQuantity())
                .price(variant.getPriceOverride())
                .build();
    }


    @Override
    public List<InventoryResponseDTO> getLowStock(int threshold) {
        List<InventoryResponseDTO> result = variantRepository.findAll().stream()
                .filter(variant -> variant.getStockQuantity() != null && variant.getStockQuantity() < threshold)
                .map(variant -> InventoryResponseDTO.builder()
                        .variantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .color(variant.getColor() != null ? variant.getColor().getName() : null)
                        .size(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                        .quantity(variant.getStockQuantity())
                        .price(variant.getPriceOverride())
                        .build())
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            throw new HttpNotFound("Không có sản phẩm nào có tồn kho dưới " + threshold);
        }

        return result;
    }
}