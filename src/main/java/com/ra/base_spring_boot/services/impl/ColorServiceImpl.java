package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.Color;
import com.ra.base_spring_boot.repository.IColorRepository;
import com.ra.base_spring_boot.services.IColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ColorServiceImpl implements IColorService {
   @Autowired
    private IColorRepository colorRepository;


    @Override
    public List<Color> findAllColors() {
        return colorRepository.findAll();
    }

    @Override
    public Color findById(Long id) {
        return colorRepository.findById(id).orElse(null);
    }

    @Override
    public Color save(Color color) {
        return colorRepository.save(color);
    }
}
