package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.ProductVariantRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantDetailDTO;
import com.ra.base_spring_boot.dto.resp.ProductVariantResponseDTO;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.services.IProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductVariantController {
    @Autowired
    private IProductVariantService productVariantService;


    @GetMapping("/product-variants/list")
    public ResponseEntity<ResponseWrapper<List<ProductVariantResponseDTO>>> findAll() {
        List<ProductVariantResponseDTO> variants = productVariantService.findAll();
        return ResponseEntity.ok(
                ResponseWrapper.<List<ProductVariantResponseDTO>>builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(variants)
                        .build()
        );
    }

    @GetMapping("/product-variants/{productId}")
    public ResponseEntity<?> getByProduct(@PathVariable Long productId) {
        List<ProductVariantResponseDTO> productVariants = productVariantService.findByProductId(productId);

        if (productVariants.isEmpty()) {
            return new ResponseEntity<>(
                    ResponseWrapper.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .code(404)
                            .data("ProductID Not Found")
                            .build(),
                    HttpStatus.NOT_FOUND
            );
        }

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(productVariants)
                        .build()
        );
    }


    @PostMapping("/admin/add")
    public ResponseEntity<ResponseWrapper<ProductVariantResponseDTO>> create(@RequestBody ProductVariantRequestDTO productVariantRequestDTO) {
        ProductVariantResponseDTO response = productVariantService.create(productVariantRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.<ProductVariantResponseDTO>builder()
                        .status(HttpStatus.CREATED)
                        .code(201)
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody ProductVariantRequestDTO dto) {
        ProductVariantResponseDTO response = productVariantService.update(id, dto);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productVariantService.delete(id);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data("Delete Successfully")
                        .build()
        );
    }


    //huỳnh gia phúc
    //chi tiết sản phẩm
    @GetMapping("/product-variants/detail/{id}")
    public ResponseEntity<?> getProductVariantDetail(@PathVariable Long id) {
        ProductVariantDetailDTO dto = productVariantService.getVariantDetail(id);
        return ResponseEntity.ok(dto);
    }
}