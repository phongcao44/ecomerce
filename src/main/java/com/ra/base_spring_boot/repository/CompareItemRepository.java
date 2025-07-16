package com.ra.base_spring_boot.repository;


import com.ra.base_spring_boot.model.CompareItem;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompareItemRepository extends JpaRepository<CompareItem, Long> {
    List<CompareItem> findAllByUser(User user);
    Optional<CompareItem> findByUserAndProduct(User user, Product product);
    void deleteByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
}
