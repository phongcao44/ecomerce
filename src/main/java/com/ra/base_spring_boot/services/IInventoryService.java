package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.InventoryResponseDTO;
import com.ra.base_spring_boot.dto.resp.InventoryUpdateRequestDTO;

import java.util.List;

public interface IInventoryService {
    List<InventoryResponseDTO> getAllInventory();

    void updateStock(InventoryUpdateRequestDTO dto);

    InventoryResponseDTO getByVariantId(Long variantId);

    List<InventoryResponseDTO> getLowStock(int threshold);
}

