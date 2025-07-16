package com.ra.base_spring_boot.services;



import com.ra.base_spring_boot.dto.resp.ProductCompareResponse;

import java.util.List;

public interface ICompareService {

    String addToCompare(Long userId, Long productId);

    String removeFromCompare(Long userId, Long productId);

    List<ProductCompareResponse> getCompareList(Long userId);
}
