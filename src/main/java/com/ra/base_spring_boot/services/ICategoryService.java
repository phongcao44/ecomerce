package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.CategoryResponse;
import com.ra.base_spring_boot.dto.resp.SearchCategoryRespone;
import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.model.constants.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICategoryService {
    List<Category> findAll();

    Page<CategoryResponse> pageable(Pageable pageable);

    Category save(Category category);

    List<CategoryResponse> pageablesub(Long parenId);

    List<SearchCategoryRespone> searchCategory(String keyword);



}
