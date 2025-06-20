package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.Size;

import java.util.List;

public interface ISizeService {
    List<Size> findAll();

    Size findById(Long id);

    Size save(Size size);
}
