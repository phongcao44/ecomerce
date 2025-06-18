package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.req.CategoryRequest;
import com.ra.base_spring_boot.dto.resp.CategoryResponse;
import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.repository.ICategoryRepository;
import com.ra.base_spring_boot.services.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    @Autowired
    public ICategoryService categoryService;
    @Autowired
    public ICategoryRepository categoryRepository;
    // list danh mục cha
    @GetMapping("/list/parent")
        public ResponseEntity<Page<CategoryResponse>> getCategories(@RequestParam(name = "page",defaultValue = "0") int page,
                                                              @RequestParam(name = "limit",defaultValue = "3") int limit,
                                                              @RequestParam(name = "sortBy",defaultValue = "id") String sortBy,
                                                              @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy
    ) {
        Sort sort =  orderBy.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<CategoryResponse> categoryPage = categoryService.pageable(pageable);
        return new ResponseEntity<>(categoryPage, HttpStatus.OK);
        }

        // danh mục con của cha
    @GetMapping("/list/son/{parentId}")
    public ResponseEntity<?> getSubCategories(@PathVariable Long parentId) {
        List<CategoryResponse> children = categoryService.pageablesub(parentId);
        if (children.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataError("Không có danh mục con", 404));
        }

        return ResponseEntity.ok(children);
    }

    //tìm kiếm
    @GetMapping("/search")
    //search?keyword=""
    public ResponseEntity<?> searchCategory(@RequestParam("keyword") String keyword){
        List<?> category = categoryService.searchCategory(keyword);
        CategoryResponse categoryResponse = new CategoryResponse(

        );
        if(category.isEmpty()){
            return new ResponseEntity<>(new DataError("sản phẩm không tồn tại",404),HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(category,HttpStatus.OK);
    }

    //add danh mục dùng chung
    @PostMapping("/add")
    public ResponseEntity<?> addCategory(@RequestBody CategoryRequest categoryRequest) {
        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());

        if (categoryRequest.getParentId() != null) {
            Optional<Category> parentCategory = categoryRepository.findById(categoryRequest.getParentId());
            if (parentCategory.isPresent()) {
                category.setParent(parentCategory.get());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Danh mục cha không tồn tại");
            }
    }else {
        category.setParent(null);
    }
        categoryRepository.save(category);
        return ResponseEntity.ok("them thanh cong goy");
    }

    //add danh mục dùng rieeng cho con
    @PostMapping("/add/son")
    public ResponseEntity<?> addCategorySon(@RequestBody CategoryRequest categoryRequest){
        // ko cho làm cha
        if (categoryRequest.getParentId() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("con cần cha không đc null");
        }
        // kiểm tra cha có ko
        Optional<Category> parentCategory = categoryRepository.findById(categoryRequest.getParentId());
        if (parentCategory.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("Danh mục cha không tồn tại");
        }

        Category category = new Category();
        category.setName(categoryRequest.getName().trim());
        category.setDescription(categoryRequest.getDescription().trim());
        category.setParent(parentCategory.get());

        categoryRepository.save(category);
        return ResponseEntity.ok("Thêm danh mục thành công");
    }

    //add danh muc danh tieng cho cha
    @PostMapping("/add/parent")
    public ResponseEntity<?> addCategoryParent(@RequestBody CategoryRequest categoryRequest){
        if (categoryRequest.getParentId() != null) {
            return ResponseEntity
                    .badRequest()
                    .body("đây là cha xóa parent đê");
        }

        // Tạo danh mục cha
        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setParent(null);
        categoryRepository.save(category);
        return ResponseEntity.ok("Thêm danh mục cha thành công");
    }
}
