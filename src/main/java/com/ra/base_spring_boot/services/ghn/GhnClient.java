package com.ra.base_spring_boot.services.ghn;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

@Component
public class GhnClient {

    @Value("${ghn.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api";

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public ResponseEntity<String> getProvinces() {
        HttpEntity<?> request = new HttpEntity<>(headers());
        return restTemplate.exchange(BASE_URL + "/master-data/province", HttpMethod.GET, request, String.class);
    }

    public ResponseEntity<String> getDistricts(Integer provinceId) {
        HttpEntity<?> request = new HttpEntity<>(headers());
        String url = BASE_URL + "/master-data/district?province_id=" + provinceId;
        return restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    }

    public ResponseEntity<String> getWards(Integer districtId) {
        HttpEntity<?> request = new HttpEntity<>(headers());
        String url = BASE_URL + "/master-data/ward?district_id=" + districtId;
        return restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    }

    public String getProvinceName(int provinceId) throws JSONException {
        JSONArray data = new JSONObject(getProvinces().getBody()).optJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject province = data.getJSONObject(i);
            if (province.getInt("ProvinceID") == provinceId) {
                return province.getString("ProvinceName");
            }
        }
        throw new RuntimeException("Province not found: " + provinceId);
    }

    public String getDistrictName(int districtId, int provinceId) throws JSONException {
        JSONArray data = new JSONObject(getDistricts(provinceId).getBody()).optJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject district = data.getJSONObject(i);
            if (district.getInt("DistrictID") == districtId) {
                return district.getString("DistrictName");
            }
        }
        throw new RuntimeException("District not found: " + districtId);
    }

    public String getWardName(String wardCode, int districtId) throws JSONException {
        JSONArray data = new JSONObject(getWards(districtId).getBody()).optJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject ward = data.getJSONObject(i);
            if (ward.getString("WardCode").equals(wardCode)) {
                return ward.getString("WardName");
            }
        }
        throw new RuntimeException("Ward not found: " + wardCode);
    }

//    public int getProvinceIdByName(String provinceName) throws JSONException {
//        JSONArray data = new JSONObject(getProvinces().getBody()).optJSONArray("data");
//        for (int i = 0; i < data.length(); i++) {
//            JSONObject province = data.getJSONObject(i);
//            if (province.getString("ProvinceName").equalsIgnoreCase(provinceName.trim())) {
//                return province.getInt("ProvinceID");
//            }
//        }
//        throw new RuntimeException("Province not found: " + provinceName);
//    }
//
//    public int getDistrictIdByName(String districtName, int provinceId) throws JSONException {
//        JSONArray data = new JSONObject(getDistricts(provinceId).getBody()).optJSONArray("data");
//        for (int i = 0; i < data.length(); i++) {
//            JSONObject district = data.getJSONObject(i);
//            if (district.getString("DistrictName").equalsIgnoreCase(districtName.trim())) {
//                return district.getInt("DistrictID");
//            }
//        }
//        throw new RuntimeException("District not found: " + districtName);
//    }
//
//    public String getWardCodeByName(String wardName, int districtId) throws JSONException {
//        JSONArray data = new JSONObject(getWards(districtId).getBody()).optJSONArray("data");
//        for (int i = 0; i < data.length(); i++) {
//            JSONObject ward = data.getJSONObject(i);
//            if (ward.getString("WardName").equalsIgnoreCase(wardName.trim())) {
//                return ward.getString("WardCode");
//            }
//        }
//        throw new RuntimeException("Ward not found: " + wardName);
//    }


    public int getProvinceIdByName(String provinceName) throws JSONException {
        JSONArray data = new JSONObject(getProvinces().getBody()).optJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject province = data.getJSONObject(i);
            if (province.getString("ProvinceName").equalsIgnoreCase(provinceName.trim())) {
                return province.getInt("ProvinceID");
            }
        }
        throw new RuntimeException("Province not found: " + provinceName);
    }

    public int getDistrictIdByName(String districtName, int provinceId) throws JSONException {
        JSONArray data = new JSONObject(getDistricts(provinceId).getBody()).optJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject district = data.getJSONObject(i);
            if (district.getString("DistrictName").equalsIgnoreCase(districtName.trim())) {
                return district.getInt("DistrictID");
            }
        }
        throw new RuntimeException("District not found: " + districtName);
    }

    public String getWardCodeByName(String wardName, int districtId) throws JSONException {
        JSONArray data = new JSONObject(getWards(districtId).getBody()).optJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject ward = data.getJSONObject(i);
            if (ward.getString("WardName").equalsIgnoreCase(wardName.trim())) {
                return ward.getString("WardCode");
            }
        }
        throw new RuntimeException("Ward not found: " + wardName);
    }


}
