package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.Size;
import com.ra.base_spring_boot.repository.ISizeRepository;
import com.ra.base_spring_boot.services.ISizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SizeServiceImpl implements ISizeService {
    @Autowired
    public ISizeRepository iSizeRepository;
    @Override
    public List<Size> findAll() {
        return iSizeRepository.findAll();
    }

    @Override
    public Size findById(Long id) {
        return iSizeRepository.findById(id).orElseThrow();
    }

    @Override
    public Size save(Size size) {
        return iSizeRepository.save(size);
    }
}
