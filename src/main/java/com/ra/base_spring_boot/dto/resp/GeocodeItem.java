package com.ra.base_spring_boot.dto.resp;

import lombok.Data;


@Data
public class GeocodeItem {
    private String title;
    private String id;
    private String resultType;
    private String localityType;
    private GeocodeAddress address;
    private Position position;
}
