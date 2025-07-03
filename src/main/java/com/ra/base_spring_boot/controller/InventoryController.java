package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.req.InventoryResponseDTO;
import com.ra.base_spring_boot.dto.req.PostRequestDTO;
import com.ra.base_spring_boot.dto.resp.InventoryUpdateRequestDTO;
import com.ra.base_spring_boot.dto.resp.PostResponseDTO;
import com.ra.base_spring_boot.services.IInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    @GetMapping("/list")
    public ResponseEntity<List<InventoryResponseDTO>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @PutMapping("/update")
    public ResponseEntity<?> importInventory(@RequestBody InventoryUpdateRequestDTO dto) {
        inventoryService.updateStock(dto);
        return ResponseEntity.ok("Nhập hàng thành công!");
    }


    @GetMapping("/product-variant/{variantId}")
    public ResponseEntity<InventoryResponseDTO> getByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(inventoryService.getByVariantId(variantId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponseDTO>> getLowStock(@RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(inventoryService.getLowStock(threshold));
    }
}

