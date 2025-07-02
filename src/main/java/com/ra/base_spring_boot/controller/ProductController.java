package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.ProductUserResponse;
import com.ra.base_spring_boot.dto.resp.ProductViewResponse;

import com.ra.base_spring_boot.dto.resp.Top5Product;
import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductView;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.ICategoryRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.services.ICategoryService;
import com.ra.base_spring_boot.services.IProductService;
import com.ra.base_spring_boot.services.IProductViewService;
import com.ra.base_spring_boot.services.impl.ProductViewServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {
    @Autowired
    private IProductService productService;
    @Autowired
    private ICategoryRepository categoryRepository;
    @Autowired
    private ICategoryService categoryService;
    @Autowired
    private IProductRepository productRepository;
    @Autowired
    private IProductViewService productViewService;


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
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<ProductResponseDTO> products = productService.pagination(pageable);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // Thêm mới Product
    @PostMapping("/admin/product/add")
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
    @PutMapping("admin/product/update/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody ProductRequestDTO dto) {
        ProductResponseDTO response = productService.update(id, dto);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return new ResponseEntity<>(new DataError("Product Not Found", 404), HttpStatus.NOT_FOUND);
    }

    // Thay đổi trạng thái Product
    @PutMapping("admin/product/change-status/{id}")
    public ResponseEntity<?> changeStatus(@PathVariable int id) {
        ProductResponseDTO response = productService.changeStatus(id);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return new ResponseEntity<>(new DataError("Product Not Found", 404), HttpStatus.NOT_FOUND);
    }

    // tìm kiếm
    @GetMapping("/product/search")
    public ResponseEntity<List<ProductResponseDTO>> search(@RequestParam(name = "keyword") String keyword) {
        List<ProductResponseDTO> products = productService.search(keyword);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // xóa sản phẩm theo id
    @DeleteMapping("admin/product/delete/{id}")
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
            return new ResponseEntity<>(new DataError("This product has variations you need to delete the variations first or is on flash sale ", 404), HttpStatus.NOT_FOUND);
        }
    }

    //Huynh Gia Phu
    //list product by category
    @GetMapping("/user/products/by-category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        Optional<Category> selectedCategoryOpt = categoryRepository.findById(categoryId);
        if (selectedCategoryOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy danh mục");
        }

        Category selectedCategory = selectedCategoryOpt.get();
        List<Long> categoryIdsToSearch = new ArrayList<>();

        // nếu là danh mục cha, lấy toàn bộ con
        if (selectedCategory.getParent() == null) {
            List<Category> children = categoryRepository.findAllByParentId(categoryId);
            categoryIdsToSearch = children.stream()
                    .map(Category::getId)
                    .toList();
        } else {
            // nếu là con thì lấy chính nó
            categoryIdsToSearch.add(categoryId);
        }

        List<Product> products = productRepository.findByCategoryIdIn(categoryIdsToSearch);

        List<ProductUserResponse> responses = products.stream()
                .map(product -> new ProductUserResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getBrand()
                        ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/products/bestSell")
    public ResponseEntity<List<?>> getBestSellProduct() {
        List<Top5Product> topProduct = productService.getTop5BestSellingProducts();
        return ResponseEntity.ok(topProduct);
    }


    @PostMapping("/{id}/view")
    public ResponseEntity<?> viewProduct(@PathVariable Long id, HttpServletRequest request) {
        productViewService.trackProductView(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/top-viewed")
    public List<ProductViewResponse> getTopViewedProducts(@RequestParam(defaultValue = "10") Long limit) {
        return productViewService.getTopViewProducts(limit);
    }

    @GetMapping("/least-viewed")
    public List<ProductViewResponse> getLeastViewedProducts(@RequestParam(defaultValue = "10") Long limit) {
        return productViewService.getLestViewProducts(limit);
    }






    @GetMapping("/products/topLeastSell")
    public ResponseEntity<List<?>> getTopLeastSellProduct() {
        List<Top5Product> topProduct = productService.getTop5LestSellingProducts();
        return ResponseEntity.ok(topProduct);
    }

}
