package com.ra.base_spring_boot.dto.resp;


import lombok.*;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProductCompareResponse {

//    private Long productId;
//    private String productName;
//    private Map<String, String> specifications;

    private String specificationName;

    private Map<String, String> productValues;

}
