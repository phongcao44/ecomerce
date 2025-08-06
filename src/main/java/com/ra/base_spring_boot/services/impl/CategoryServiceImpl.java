package com.ra.base_spring_boot.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ra.base_spring_boot.dto.resp.CategoryDetailResponse;
import com.ra.base_spring_boot.dto.resp.CategoryFlatResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements ICategoryService {
    @Autowired
    public ICategoryRepository categoryRepository;

    @Autowired
    public Cloudinary cloudinary;


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
                                .parentId(null) // chỉ lấy cha
                                .image(category.getIcon())
                                .slug(category.getSlug())
                                .build()
                ).collect(Collectors.toList());
        return new PageImpl<>(responesedto, pageable, responesedto.size());
    }

    @Override
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public List<CategoryResponse> pageablesub(Long parentId) {
        List<Category> subcategory = categoryRepository.findAllByParentId(parentId);
        System.out.println(subcategory.get(0).getIcon() + "==================");
        return subcategory.stream().map(category ->
                CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .slug(category.getSlug())
                        .description(category.getDescription())
                        .parentId(category.getParent() != null ? category.getParent().getId() : null)
                        .image(category.getIcon())
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
    public List<CategoryDetailResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();

        Map<Long, CategoryDetailResponse> categoryMap = new HashMap<>();
        List<CategoryDetailResponse> roots = new ArrayList<>();

        for (Category cat : allCategories) {
            categoryMap.put(cat.getId(), CategoryDetailResponse.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .slug(cat.getSlug())
                    .description(cat.getDescription())
                    .image(cat.getIcon())
                    .parentId(cat.getParent() != null ? cat.getParent().getId() : null)
                    .children(new ArrayList<>())
                    .build());
        }

        for (CategoryDetailResponse dto : categoryMap.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CategoryDetailResponse parent = categoryMap.get(dto.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }

        return roots;
    }

    @Override
    public List<CategoryResponse> findAllParents(Long sonId) {
        List<CategoryResponse> result = new ArrayList<>();
        Optional<Category> optional = categoryRepository.findById(sonId);

        while (optional.isPresent()) {
            Category current = optional.get();
            result.add(CategoryResponse.builder()
                    .id(current.getId())
                    .name(current.getName())
                    .slug(current.getSlug())
                    .description(current.getDescription())
                    .parentId(current.getParent() != null ? current.getParent().getId() : null)
                    .level(getCategoryLevel(current))
                    .image(current.getIcon())
                    .build());

            optional = Optional.ofNullable(current.getParent());
        }

        return result;
    }

    @Override
    public List<CategoryFlatResponse> getFlattenCategoryList() {
        List<Category> allCategories = categoryRepository.findAll();
        List<CategoryFlatResponse> flatList = new ArrayList<>();

        for (Category category : allCategories) {
            int level = 1;
            Category current = category.getParent();
            while (current != null) {
                level++;
                current = current.getParent();
            }

            flatList.add(CategoryFlatResponse.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .slug(category.getSlug())
                    .description(category.getDescription())
                    .level(level)
                    .parentId(category.getParent() != null ? category.getParent().getId() : null)
                    .parentName(category.getParent() != null ? category.getParent().getName() : null)
                    .image(category.getIcon())
                    .build());
        }

        // Sắp xếp theo level (nếu cần)
        flatList.sort(Comparator.comparingInt(CategoryFlatResponse::getLevel));

        return flatList;
    }

    public int getCategoryLevel(Category category) {
        int level = 1;
        Category current = category;

        while (current.getParent() != null) {
            level++;
            current = current.getParent();
        }

        return level;
    }
}
