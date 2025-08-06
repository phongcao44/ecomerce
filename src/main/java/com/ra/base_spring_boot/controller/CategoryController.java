package com.ra.base_spring_boot.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.req.AddParentCategoryRequest;
import com.ra.base_spring_boot.dto.req.CategoryRequest;
import com.ra.base_spring_boot.dto.resp.CategoryDetailResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1")
public class CategoryController {
    @Autowired
    public ICategoryService categoryService;
    @Autowired
    public ICategoryRepository categoryRepository;
    @Autowired
    public Cloudinary cloudinary;

    // list danh mục cha
    @GetMapping("/categories/list/parent")
    public ResponseEntity<Page<CategoryResponse>> getCategories(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                @RequestParam(name = "limit", defaultValue = "3") int limit,
                                                                @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
                                                                @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<CategoryResponse> categoryPage = categoryService.pageable(pageable);
        return new ResponseEntity<>(categoryPage, HttpStatus.OK);
    }

    //list tìm cha va ong noi
    @GetMapping("/list/son_of_parent/{sonId}")
    public ResponseEntity<?> getson(@PathVariable Long sonId) {
        List<CategoryResponse> parentLine = categoryService.findAllParents(sonId);
        if (parentLine.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy danh mục con");
        }
        return ResponseEntity.ok(parentLine);
    }

    // danh mục con của cha
    @GetMapping("/categories/list/son/{parentId}")
    public ResponseEntity<?> getSubCategories(@PathVariable Long parentId) {
        List<CategoryResponse> children = categoryService.pageablesub(parentId);
        if (children.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataError("Không có danh mục con", 404));
        }

        return ResponseEntity.ok(children);
    }

    //tìm kiếm
    @GetMapping("/categories/search")
    //search?keyword=""
    public ResponseEntity<?> searchCategory(@RequestParam("keyword") String keyword) {
        List<?> category = categoryService.searchCategory(keyword);
        CategoryResponse categoryResponse = new CategoryResponse(

        );
        if (category.isEmpty()) {
            return new ResponseEntity<>(new DataError("sản phẩm không tồn tại", 404), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(category, HttpStatus.OK);
    }

    private Map uploadImageToCloudinary(MultipartFile file) {
        try {
            return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "category"
            ));
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }
    //add danh mục dùng chung


    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addCategory(@ModelAttribute CategoryRequest categoryRequest) {
        Category category = new Category();

        // Generate slug from name
        String slug = generateSlug(categoryRequest.getName());

        // Upload ảnh từ categoryRequest.getImage()
        Map uploadResult = uploadImageToCloudinary(categoryRequest.getImage());
        category.setName(categoryRequest.getName());
        category.setSlug(slug);
        category.setDescription(categoryRequest.getDescription());
        category.setIcon(uploadResult.get("secure_url").toString());

        if (categoryRequest.getParentId() != null) {
            Optional<Category> parentCategory = categoryRepository.findById(categoryRequest.getParentId());
            if (parentCategory.isPresent()) {
                category.setParent(parentCategory.get());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Danh mục cha không tồn tại");
            }
        } else {
            category.setParent(null);
        }

        categoryRepository.save(category);
        return ResponseEntity.ok("Thêm thành công rồi");
    }

    private String generateSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // remove accents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")     // Remove special characters
                .replaceAll("\\s+", "-")             // Replace spaces with hyphens
                .replaceAll("-+", "-")               // Merge multiple hyphens
                .replaceAll("^-|-$", "");            // Remove hyphen at the ends
        // Ensure uniqueness by appending a number if needed
        String baseSlug = slug;
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
    //add danh mục dùng rieeng cho con
    @PostMapping("/admin/categories/add/son/{parentId}")
    public ResponseEntity<?> addCategorySon(@PathVariable Long parentId, @RequestBody AddParentCategoryRequest categoryRequest) {
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
    @PostMapping("/admin/categories/add/parent")
    public ResponseEntity<?> addCategoryParent(@RequestBody AddParentCategoryRequest addParentCategoryRequestRequest) {
        // Tạo danh mục cha
        if (categoryRepository.existsByName(addParentCategoryRequestRequest.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }
        Category category = new Category();
        category.setName(addParentCategoryRequestRequest.getName());
        category.setDescription(addParentCategoryRequestRequest.getDescription());
        category.setParent(null); // danh mục cha
        categoryRepository.save(category);
        return ResponseEntity.ok("Thêm danh mục cha thành công");
    }

    //sửa danh mục cha
    @PutMapping(value = "/admin/categories/edit/parent/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateParentCategory(
            @PathVariable Long id,
            @ModelAttribute AddParentCategoryRequest categoryRequest) {

        Optional<Category> categoryOptional = categoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DataError("Không tìm thấy danh mục cha", 404));
        }

        Category category = categoryOptional.get();

        // Kiểm tra không phải là danh mục cha
        if (category.getParent() != null) {
            return ResponseEntity.badRequest().body("Danh mục này không phải là danh mục cha");
        }

        // Kiểm tra tên mới có bị trùng (nếu tên thay đổi)
        if (!category.getName().equals(categoryRequest.getName())
                && categoryRepository.existsByName(categoryRequest.getName())) {
            return ResponseEntity.badRequest().body("Tên danh mục đã tồn tại");
        }

        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());

        // Nếu có ảnh mới thì upload và cập nhật icon
        if (categoryRequest.getImage() != null && !categoryRequest.getImage().isEmpty()) {
            Map uploadResult = uploadImageToCloudinary(categoryRequest.getImage());
            category.setIcon(uploadResult.get("secure_url").toString());
        }

        categoryRepository.save(category);

        return ResponseEntity.ok("Cập nhật danh mục cha thành công");
    }


    // sửa con
    @PutMapping(value = "/admin/categories/edit/son/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSubCategory(
            @PathVariable Long id,
            @ModelAttribute CategoryRequest categoryRequest) {

        Optional<Category> categoryOptional = categoryRepository.findById(id);

        if (categoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DataError("Không tìm thấy danh mục con", 404));
        }

        Category category = categoryOptional.get();

        // Kiểm tra nếu không có parent, tức là không phải danh mục con
        if (category.getParent() == null) {
            return ResponseEntity.badRequest().body("Danh mục này không phải danh mục con");
        }

        // Kiểm tra danh mục cha mới (nếu có)
        if (categoryRequest.getParentId() != null) {
            Optional<Category> parentOptional = categoryRepository.findById(categoryRequest.getParentId());
            if (parentOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Danh mục cha không tồn tại");
            }

            // Không được gán chính nó làm cha
            if (categoryRequest.getParentId().equals(id)) {
                return ResponseEntity.badRequest().body("Không thể gán chính nó làm danh mục cha");
            }

            category.setParent(parentOptional.get());
        }

        category.setName(categoryRequest.getName().trim());
        category.setDescription(categoryRequest.getDescription().trim());

        // Nếu có ảnh mới thì upload
        if (categoryRequest.getImage() != null && !categoryRequest.getImage().isEmpty()) {
            Map uploadResult = uploadImageToCloudinary(categoryRequest.getImage());
            category.setIcon(uploadResult.get("secure_url").toString());
        }

        categoryRepository.save(category);

        return ResponseEntity.ok("Cập nhật danh mục con thành công");
    }


    //dele cha
    @DeleteMapping("/admin/categories/delete/parent/{id}")
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
    @DeleteMapping("/admin/categories/delete/son/{id}")
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

    @GetMapping("/categories/tree")
    public ResponseEntity<?> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/categories/flat")
    public ResponseEntity<?> getFlatCategoryList() {
        return ResponseEntity.ok(categoryService.getFlattenCategoryList());
    }


}
