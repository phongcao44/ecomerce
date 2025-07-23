package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.dto.resp.CategoryResponse;
import com.ra.base_spring_boot.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ICategoryRepository extends JpaRepository<Category,Long> {
    Page<Category> findAllByParentIsNull(Pageable pageable);

    List<Category> findAllByParentId(Long parentId);

    List<Category> findCategoriesByNameContainingIgnoreCase(String keyword);

    List<Category> findByParent(Category parent);

    List<Category> findByParentIsNull();

    boolean existsByName(String name);

    List<Category> findByParentId(Long parentId);
}
