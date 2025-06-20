package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;



import java.util.List;

public interface IProductService {

    List<ProductResponseDTO> findAll();

    ProductResponseDTO findById(long id);

    ProductResponseDTO save(ProductRequestDTO dto);

    ProductResponseDTO changeStatus(long id);


    // cập nhật Product
    ProductResponseDTO update(long id, ProductRequestDTO dto);

    // phân trang
    Page<ProductResponseDTO> pagination(Pageable pageable);

    List<ProductResponseDTO> search(String keyword);

    void delete(Long id);

}

