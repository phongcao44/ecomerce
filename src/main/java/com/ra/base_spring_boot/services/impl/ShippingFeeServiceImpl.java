package com.ra.base_spring_boot.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.DistributionCenter;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.ShippingFee;
import com.ra.base_spring_boot.repository.DistributionCenterRepository;
import com.ra.base_spring_boot.repository.IAddressRepository;
import com.ra.base_spring_boot.repository.ShippingFeeRepository;
import com.ra.base_spring_boot.services.ShippingFeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ShippingFeeServiceImpl implements ShippingFeeService {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    ShippingFeeRepository shippingFeeRepository;
    @Autowired
    DistributionCenterRepository distributionCenterRepository;
    @Autowired
    IAddressRepository addressRepository;

    private final String GHN_TOKEN = "3bffc186-56f0-11f0-a89f-2e7d777c887f";
    private final String GHN_SHOP_ID = "5868452";

    @Override
    public ShippingFee calculateAndSaveShippingFee(Long userId, Address address) {
//        DistributionCenter center = distributionCenterRepository.findByUser_Id(userId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy Distribution Center"));
//
//        Integer fromDistrictId = center.getDistributor();
//        String fromWardCode = center.getWard();


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", GHN_TOKEN);
        headers.set("ShopId", GHN_SHOP_ID);

        Map<String, Object> body = new HashMap<>();
        body.put("from_district_id", 1462);
        body.put("from_ward_code", "21605");
        body.put("service_id", 53320);
        body.put("service_type_id", 1);
        body.put("to_district_id", address.getDistrictId());
        body.put("to_ward_code", address.getWardCode());
        body.put("height", 15);
        body.put("length", 22);
        body.put("weight", 1000);
        body.put("width", 22);
        body.put("insurance_value", 10);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee",
                request,
                JsonNode.class
        );

        JsonNode data = response.getBody().get("data");

        ShippingFee shippingFee = ShippingFee.builder()
                .total(data.get("total").asInt())
                .build();

        return shippingFeeRepository.save(shippingFee);
    }
}
