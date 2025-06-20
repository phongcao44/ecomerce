package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;


public interface IBannerRepository extends JpaRepository<Banner, Integer> {
}
