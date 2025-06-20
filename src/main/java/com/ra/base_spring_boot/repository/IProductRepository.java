package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IProductRepository extends JpaRepository<Product, Long> {
    List<Product> findProductByNameContainsIgnoreCase(String name);
}
