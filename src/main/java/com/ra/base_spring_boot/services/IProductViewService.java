package com.ra.base_spring_boot.services;

import jakarta.servlet.http.HttpServletRequest;

public interface IProductViewService {
    void trackProductView(Long productId, HttpServletRequest request);
}
