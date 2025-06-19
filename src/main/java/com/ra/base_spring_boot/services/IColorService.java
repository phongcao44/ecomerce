package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.Color;

import java.util.List;

public interface IColorService {
    List<Color> findAllColors();

    Color findById(Long id);

    Color save(Color color);
}
