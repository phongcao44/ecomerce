package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.Top5Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.util.List;

public interface IProductService {

    List<ProductResponseDTO> findAll();

    ProductResponseDTO findById(long id);

    ProductResponseDTO save(ProductRequestDTO dto);

    ProductResponseDTO changeStatus(long id);


    // cập nhật Product
    ProductResponseDTO update(long id, ProductRequestDTO dto);

    // phân trang
    Page<ProductResponseDTO> pagination(Pageable pageable, String keyword, String status);

    List<ProductResponseDTO> search(String keyword);

    void delete(Long id);

    //    List<Top5Product> getTop5BestSellingProducts();
    List<ProductResponseDTO> getTop5BestSellingProducts();

    List<Top5Product> getTop5LestSellingProducts();

    Page<ProductResponseDTO> getProductsPaginate(
            String keyword,
            Long categoryId,
            String categorySlug,
            String status,
            String brandName,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Integer minRating,
            int page,
            int limit,
            String sortBy,
            String orderBy
    );

    ProductResponseDTO findByName(String productName);

    ProductResponseDTO findBySlug(String slug);

    List<ProductResponseDTO> getRelatedProducts(Long productId);
}

