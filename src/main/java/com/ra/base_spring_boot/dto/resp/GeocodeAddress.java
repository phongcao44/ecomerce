package com.ra.base_spring_boot.dto.resp;

import lombok.Data;

@Data
public class GeocodeAddress {
    private String label;
    private String countryCode;
    private String countryName;
    private String county;
    private String city;
    private String district;
    private String postalCode;
}
