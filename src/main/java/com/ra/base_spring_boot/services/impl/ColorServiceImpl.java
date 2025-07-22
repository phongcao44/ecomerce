package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.ColorDTO;
import com.ra.base_spring_boot.model.Color;
import com.ra.base_spring_boot.repository.IColorRepository;
import com.ra.base_spring_boot.services.IColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ColorServiceImpl implements IColorService {
   @Autowired
    private IColorRepository colorRepository;


    @Override
    public List<ColorDTO> findAllColors() {
        return colorRepository.findAll().stream()
                .map(color -> ColorDTO.builder()
                        .id(color.getId())
                        .name(color.getName())
                        .hex_code(color.getHexCode())
                        .build())
                .collect(Collectors.toList());
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
