package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISizeRepository extends JpaRepository<Size,Long> {
}
