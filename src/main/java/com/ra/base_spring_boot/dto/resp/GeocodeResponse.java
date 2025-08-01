package com.ra.base_spring_boot.dto.resp;

import lombok.Data;

import javax.swing.text.Position;
import java.util.List;

@Data
public class GeocodeResponse {
    private List<GeocodeItem> items;
}
