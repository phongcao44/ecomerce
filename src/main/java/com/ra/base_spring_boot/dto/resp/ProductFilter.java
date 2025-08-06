package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProductFilter {
    private String category;    // Danh mục
    private String brand;       // Thương hiệu
    private Integer minPrice;   // Giá thấp nhất
    private Integer maxPrice;   // Giá cao nhất
    private Integer minRating;  // Đánh giá từ bao nhiêu sao trở lên
}
