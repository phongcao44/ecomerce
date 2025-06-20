package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.services.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {
    @Autowired
    private IProductService productService;

    // hiển thị danh sách Product
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> index() {
        List<ProductResponseDTO> products = productService.findAll();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ProductResponseDTO product = productService.findById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        }
        return new ResponseEntity<>(new DataError("Product Not Found", 404), HttpStatus.NOT_FOUND);
    }


    // phân trang Product, sắp xếp
    @GetMapping("/paginate")
    public ResponseEntity<Page<ProductResponseDTO>> getAllPaginate(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "3") int limit,
            @RequestParam(name = "sortBy", defaultValue = "price") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy){
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<ProductResponseDTO> products = productService.pagination(pageable);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // Thêm mới Product
    @PostMapping("/add")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        ProductResponseDTO response = productService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.builder()
                        .status(HttpStatus.CREATED)
                        .code(201)
                        .data(response)
                        .build()
        );
    }


    // Cập nhật Product
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody ProductRequestDTO dto) {
        ProductResponseDTO response = productService.update(id, dto);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return new ResponseEntity<>(new DataError("Product Not Found", 404), HttpStatus.NOT_FOUND);
    }

    // Thay đổi trạng thái Product
    @PutMapping("/change-status/{id}")
    public ResponseEntity<?> changeStatus(@PathVariable int id) {
        ProductResponseDTO response = productService.changeStatus(id);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return new ResponseEntity<>(new DataError("Product Not Found", 404), HttpStatus.NOT_FOUND);
    }

// tìm kiếm
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> search(@RequestParam(name = "keyword") String keyword) {
        List<ProductResponseDTO> products = productService.search(keyword);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // xóa sản phẩm theo id
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .status(HttpStatus.OK)
                            .code(200)
                            .data("Delete product successfully")
                            .build()
            );
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new DataError("Product Not Found", 404), HttpStatus.NOT_FOUND);
        }
    }


}
