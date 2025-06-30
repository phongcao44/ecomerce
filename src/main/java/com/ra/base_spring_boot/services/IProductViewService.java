package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.ProductViewResponse;
import com.ra.base_spring_boot.model.ProductView;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IProductViewService {
    void trackProductView(Long productId, HttpServletRequest request);

    List<ProductViewResponse> getTopViewProducts(Long limit);

    List<ProductViewResponse> getLestViewProducts(Long limit);
}
