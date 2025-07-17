package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.ProductSpecificationRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductSpecificationResponseDTO;
import com.ra.base_spring_boot.services.ProductSpecificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/product-specifications")
@RequiredArgsConstructor
public class ProductSpecificationController {

    private final ProductSpecificationService productSpecificationService;

    @GetMapping("/list")
    public List<ProductSpecificationResponseDTO> getAll() {
        return productSpecificationService.getAll();
    }

    @GetMapping("/product/product{id}")
    public List<ProductSpecificationResponseDTO> getByProduct(@PathVariable Long id) {
        return productSpecificationService.getByProductId(id);
    }

    @PostMapping("/add")
    public ProductSpecificationResponseDTO create(@RequestBody ProductSpecificationRequestDTO dto) {
        return productSpecificationService.create(dto);
    }

    @PutMapping("/update/{id}")
    public ProductSpecificationResponseDTO update(@PathVariable Long id, @RequestBody ProductSpecificationRequestDTO dto) {
        return productSpecificationService.update(id, dto);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        productSpecificationService.delete(id);
    }
}
