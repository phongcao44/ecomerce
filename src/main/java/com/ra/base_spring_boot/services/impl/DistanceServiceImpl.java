package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.DistanceInfoResponse;
import com.ra.base_spring_boot.dto.resp.GeocodeItem;
import com.ra.base_spring_boot.dto.resp.GeocodeResponse;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.Location;
import com.ra.base_spring_boot.repository.IAddressRepository;
import com.ra.base_spring_boot.repository.LocationRepository;
import com.ra.base_spring_boot.services.DistanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ra.base_spring_boot.dto.resp.Position;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DistanceServiceImpl implements DistanceService {
    private final IAddressRepository addressRepository;
    private final LocationRepository locationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEOCODE_API = "https://geocode.search.hereapi.com/v1/geocode";
    private static final String ROUTE_API = "https://router.hereapi.com/v8/routes";
    private static final String API_KEY = "Ao97kismDBtgnSIbhEnEIjetXmlIV4NI7C4eQzUQip8";
    @SuppressWarnings("unchecked")
    @Override
    public DistanceInfoResponse calculateDistanceFromUserToOrderAddress(Long userId, Long orderId) {
        Address address = addressRepository.findShippingAddressByOrderId(orderId);
        if (address == null) throw new RuntimeException("Không tìm thấy địa chỉ");

        Location location = locationRepository.findTopByUserIdOrderByTimestampDesc(userId);
        if (location == null) throw new RuntimeException("Không tìm thấy vị trí người dùng");

        String ward = ensurePrefix(address.getWard(), "xã|phường|thị trấn");
        String district = ensurePrefix(address.getDistrict(), "huyện|quận|thành phố");
        String province = ensurePrefix(address.getProvince(), "tỉnh|thành phố");
        if (province.equalsIgnoreCase("hồ chí minh")) {
            province = "Thành phố Hồ Chí Minh";
        }

        String query = String.format("%s, %s, %s", ward, district, province);
        URI geocodeUri = UriComponentsBuilder.fromHttpUrl(GEOCODE_API)
                .queryParam("q", query)
                .queryParam("apiKey", API_KEY)
                .build()
                .encode()
                .toUri();

        System.err.println(" GEOCODE QUERY: " + query);
        System.err.println(" GEOCODE URL: " + geocodeUri);

        ResponseEntity<GeocodeResponse> response = restTemplate.getForEntity(geocodeUri, GeocodeResponse.class);

        if (response.getBody() == null || response.getBody().getItems() == null || response.getBody().getItems().isEmpty()) {
            throw new RuntimeException("Không lấy được tọa độ từ địa chỉ: " + query);
        }

        Position shippingPos = response.getBody().getItems().get(0).getPosition();
        Position userPos = new Position(location.getLatitude(), location.getLongitude());

        String routeUrl = UriComponentsBuilder.fromHttpUrl(ROUTE_API)
                .queryParam("transportMode", "car")
                .queryParam("origin", userPos.getLat() + "," + userPos.getLng())
                .queryParam("destination", shippingPos.getLat() + "," + shippingPos.getLng())
                .queryParam("return", "summary")
                .queryParam("apiKey", API_KEY)
                .toUriString();

        System.out.println(" ROUTE URL: " + routeUrl);

        ResponseEntity<Map> routeResponse = restTemplate.getForEntity(routeUrl, Map.class);
        List<Map<String, Object>> routes = (List<Map<String, Object>>) routeResponse.getBody().get("routes");

        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("Không tính được khoảng cách giữa 2 điểm.");
        }

        Map<String, Object> summary = (Map<String, Object>)
                ((Map<String, Object>) ((List<?>) routes.get(0).get("sections")).get(0)).get("summary");

        Double distance = ((Number) summary.get("length")).doubleValue();

        return new DistanceInfoResponse(userPos, shippingPos, distance);
    }


    private String ensurePrefix(String input, String type) {
        if (input == null || input.trim().isEmpty()) return "";

        input = input.trim().toLowerCase();
        switch (type.toLowerCase()) {
            case "xã|phường|thị trấn":
                if (!input.startsWith("xã") && !input.startsWith("phường") && !input.startsWith("thị trấn")) {
                    return "xã " + input;
                }
                break;
            case "huyện|quận|thành phố":
                if (!input.startsWith("huyện") && !input.startsWith("quận") && !input.startsWith("thành phố")) {
                    return "huyện " + input;
                }
                break;
            case "tỉnh|thành phố":
                if (!input.startsWith("tỉnh") && !input.startsWith("thành phố")) {
                    return "tỉnh " + input;
                }
                break;
        }
        return input;
    }


}
