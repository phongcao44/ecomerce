package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IColorRepository extends JpaRepository<Color, Long> {
}
