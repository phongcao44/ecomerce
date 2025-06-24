package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.CategoryResponse;
import com.ra.base_spring_boot.dto.resp.SearchCategoryRespone;
import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.repository.ICategoryRepository;
import com.ra.base_spring_boot.services.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements ICategoryService {
    @Autowired
    public ICategoryRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Page<CategoryResponse> pageable(Pageable pageable) {
        Page<Category> categoryPage = categoryRepository.findAllByParentIsNull(pageable);
        List<CategoryResponse> responesedto;
        responesedto = categoryPage.stream()
                .filter(category -> category.getParent() == null) // Chỉ lấy danh mục cha
                .map(category ->
                CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .parentId(null)// chỉ lấy cha
                        .build()
                ).collect(Collectors.toList());
        return new PageImpl<>(responesedto, pageable, responesedto.size());
    }

    @Override
    public List<CategoryResponse> pageablesub(Long  parentId) {
        List<Category> subcategory = categoryRepository.findAllByParentId(parentId);
        return subcategory.stream().map(category ->
                        CategoryResponse.builder()
                                .id(category.getId())
                                .name(category.getName())
                                .description(category.getDescription())
                                .parentId(category.getParent().getId())
                                .build()
                        ).collect(Collectors.toList());
    }

    @Override
    public List<SearchCategoryRespone> searchCategory(String keyword) {
        return categoryRepository.findCategoriesByNameContainingIgnoreCase(keyword)
                .stream()
                .map(category -> new SearchCategoryRespone(
                        category.getName(),
                        category.getDescription()
                )).collect(Collectors.toList());
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public Category save(Category category) {
        return null;
    }
}
