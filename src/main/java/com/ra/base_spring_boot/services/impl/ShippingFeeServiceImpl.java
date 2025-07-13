package com.ra.base_spring_boot.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ra.base_spring_boot.model.Address;
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
    private RestTemplate restTemplate;
    @Autowired
    private ShippingFeeRepository shippingFeeRepository;
    @Autowired
    private DistributionCenterRepository distributionCenterRepository;
    @Autowired
    private IAddressRepository addressRepository;

    private final String GHN_TOKEN = "3bffc186-56f0-11f0-a89f-2e7d777c887f";
    private final String GHN_SHOP_ID = "5868452";

    private static final int FROM_DISTRICT_ID = 1462; // Kho cố định
    private static final String FROM_WARD_CODE = "21605"; // Ward của kho

    @Override
    public ShippingFee calculateAndSaveShippingFee(Long userId, Address address) {
        // Bước 1: Lấy service_id tương ứng
        int serviceId = getServiceId(address.getDistrictId());

        // Bước 2: Gọi API tính phí vận chuyển
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", GHN_TOKEN);
        headers.set("ShopId", GHN_SHOP_ID);

        Map<String, Object> body = new HashMap<>();
        body.put("from_district_id", FROM_DISTRICT_ID);
        body.put("from_ward_code", FROM_WARD_CODE);
        body.put("to_district_id", address.getDistrictId());
        body.put("to_ward_code", address.getWardCode());
        body.put("height", 15);
        body.put("length", 22);
        body.put("weight", 1000);
        body.put("width", 22);
        body.put("insurance_value", 1000000);
        body.put("service_id", serviceId);
        body.put("service_type_id", 1); // GHN yêu cầu, giữ nguyên

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee",
                request,
                JsonNode.class
        );

        JsonNode data = response.getBody().get("data");

        if (data == null || data.get("total") == null) {
            throw new RuntimeException("Không thể tính được phí vận chuyển.");
        }

        ShippingFee shippingFee = ShippingFee.builder()
                .total(data.get("total").asInt())
                .build();

        return shippingFeeRepository.save(shippingFee);
    }

    private int getServiceId(int toDistrictId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", GHN_TOKEN);
        headers.set("ShopId", GHN_SHOP_ID);

        Map<String, Object> body = new HashMap<>();
        body.put("shop_id", Integer.parseInt(GHN_SHOP_ID));
        body.put("from_district", FROM_DISTRICT_ID);
        body.put("to_district", toDistrictId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services",
                request,
                JsonNode.class
        );

        JsonNode services = response.getBody().get("data");

        if (services != null && services.isArray() && services.size() > 0) {
            return services.get(0).get("service_id").asInt();
        } else {
            throw new RuntimeException("GHN không hỗ trợ tuyến giao hàng này.");
        }
    }
}
