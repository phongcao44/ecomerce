package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.ProductImageRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductImageResponseDTO;
import com.ra.base_spring_boot.services.IProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductImageController {
    private final IProductImageService productImageService;

    @GetMapping("/product-image/list")
    public ResponseEntity<?> getAllImages() {
        return ResponseEntity.ok(productImageService.findAll());
    }


    @PostMapping(value = "/admin/product-image/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImage(@ModelAttribute ProductImageRequestDTO dto) {
        ProductImageResponseDTO image = productImageService.create(dto);
        return ResponseEntity.ok(image);
    }

    @PutMapping(value = "/admin/product-image/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponseDTO> updateImage(@PathVariable Long id,
            @ModelAttribute ProductImageRequestDTO dto) {
        return ResponseEntity.ok(productImageService.update(id, dto));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProduct(@PathVariable Long productId) {
        List<ProductImageResponseDTO> productImage = productImageService.getByProductId(productId);

        if (productImage.isEmpty()) {
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
                        .data(productImage)
                        .build()
        );
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<?> getByVariant(@PathVariable Long variantId) {
        List<ProductImageResponseDTO> productImage = productImageService.getByVariantId(variantId);

        if (productImage.isEmpty()) {
            return new ResponseEntity<>(
                    ResponseWrapper.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .code(404)
                            .data("ProductVariantID Not Found")
                            .build(),
                    HttpStatus.NOT_FOUND
            );
        }
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(productImage)
                        .build()
        );
    }

    @DeleteMapping("/admin/product-image/delete{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productImageService.delete(id);
        return ResponseEntity.ok("Deleted image with ID: " + id);
    }
}

