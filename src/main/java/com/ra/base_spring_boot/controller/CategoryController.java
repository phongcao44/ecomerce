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
    @PostMapping("/add/son/{parentId}")
    public ResponseEntity<?> addCategorySon(@PathVariable Long parentId,@RequestBody CategoryRequest categoryRequest){
        // Kiểm tra cha có tồn tại không
        Optional<Category> parentCategory = categoryRepository.findById(parentId);
        if (parentCategory.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("chưa có ba");
        }

        Category category = new Category();
        category.setName(categoryRequest.getName().trim());
        category.setDescription(categoryRequest.getDescription().trim());
        category.setParent(parentCategory.get());

        categoryRepository.save(category);
        return ResponseEntity.ok("Thêm danh mục con thành công");
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

    //sửa danh mục cha
    @PutMapping("/edit/parent/{id}")
    public ResponseEntity<?> updateParentCategory(@PathVariable Long id, @RequestBody CategoryRequest categoryRequest) {
        Optional<Category> categoryOptional = categoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DataError("Không tìm thấy danh mục cha", 404));
        }

        Category category = categoryOptional.get();

        // Kiểm tra có phaỉ llà cha khong
        if (category.getParent() != null) {
            return ResponseEntity.badRequest().body("Danh mục này không phải là danh mục cha");
        }

        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());

        categoryRepository.save(category);

        return ResponseEntity.ok("Cập nhật danh mục cha thành công");
    }

    // sửa con
    @PutMapping("/edit/son/{id}")
    public ResponseEntity<?> updateSubCategory(@PathVariable Long id, @RequestBody CategoryRequest categoryRequest) {
        Optional<Category> categoryOptional = categoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataError("Không tìm thấy danh mục con", 404));
        }

        Category category = categoryOptional.get();

        // Kiểm tra nếu không có parent, tức là nó không phải danh mục con
        if (category.getParent() == null) {
            return ResponseEntity.badRequest().body("Danh mục này không phải danh mục con");
        }

        // Kiểm tra danh mục cha mới có tồn tại không
        if (categoryRequest.getParentId() != null) {
            Optional<Category> parentOptional = categoryRepository.findById(categoryRequest.getParentId());
            if (parentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Danh mục cha không tồn tại");
            }
            category.setParent(parentOptional.get());
        }

        category.setName(categoryRequest.getName().trim());
        category.setDescription(categoryRequest.getDescription().trim());

        categoryRepository.save(category);

        return ResponseEntity.ok("Cập nhật danh mục con thành công");
    }


    //dele cha
    @DeleteMapping("/delete/parent/{id}")
    public ResponseEntity<?> deleteParentCategory(@PathVariable Long id) {
        Optional<Category> categoryOptional = categoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataError("Không tìm thấy danh mục cha", 404));
        }

        Category category = categoryOptional.get();


        if (category.getParent() != null) {
            return ResponseEntity.badRequest()
                    .body("đâu phải cha");
        }

        List<Category> subCategories = categoryRepository.findByParent(category);
        if (!subCategories.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("xóa con trc hãy xóa cha");
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Xóa danh mục cha thành công");
    }

    //xo con
    @DeleteMapping("/delete/son/{id}")
    public ResponseEntity<?> deleteSubCategory(@PathVariable Long id) {
        Optional<Category> categoryOptional = categoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataError("Không tìm thấy danh mục", 404));
        }

        Category category = categoryOptional.get();

        if (category.getParent() == null) {
            return ResponseEntity.badRequest()
                    .body("đây là cha không phải con");
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.ok("Xóa danh mục con thành công");
    }
}
