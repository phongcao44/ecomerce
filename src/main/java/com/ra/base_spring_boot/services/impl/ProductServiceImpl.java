package com.ra.base_spring_boot.services.impl;


import com.ra.base_spring_boot.dto.req.ProductRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpForbiden;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.dto.resp.Top5Product;
import com.ra.base_spring_boot.model.Category;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.ReturnPolicy;
import com.ra.base_spring_boot.model.constants.ProductStatus;
import com.ra.base_spring_boot.repository.ICategoryRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IReturnPolicyRepository;
import com.ra.base_spring_boot.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    @Autowired
    private final IProductRepository productRepository;
    @Autowired
    private final ICategoryRepository categoryRepository;

    private final IReturnPolicyRepository returnPolicyRepository;


    @Override
    public List<ProductResponseDTO> findAll() {
        List<Product> products = productRepository.findAll();
        // Convert Entity => DTO
        List<ProductResponseDTO> responseDTOS;
        responseDTOS = products.stream().map(product ->
                ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .status(product.getStatus())
                        .categoryName(product.getCategory().getName())
                        .build()
        ).collect(Collectors.toList());
        return responseDTOS;

    }

    @Override
    public ProductResponseDTO findById(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .status(product.getStatus())
                .brand(product.getBrand())
                .categoryName(product.getCategory().getName())
                .build();

    }

    @Override
    public ProductResponseDTO save(ProductRequestDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new HttpNotFound("Category Not Found"));

        ReturnPolicy returnPolicy = returnPolicyRepository.findById(dto.getReturn_policy_id())
                .orElseThrow(() -> new HttpNotFound("Return Policy Not Found"));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .brand(dto.getBrand())
                .status(dto.getStatus())
                .category(category)
                .returnPolicy(returnPolicy)
                .build();

        product = productRepository.save(product);

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .status(product.getStatus())
                .categoryName(product.getCategory().getName())
                .build();
    }


    @Override
    public ProductResponseDTO changeStatus(long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) return null;

        // Chỉ chuyển đổi giữa Active và InActive
        if (product.getStatus() == ProductStatus.IN_STOCK) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.IN_STOCK);
        } else {
            // Không thay đổi nếu là OUT_OF_STOCK hoặc DISCONTINUED
            return null;
        }

        Product updated = productRepository.save(product);

        return ProductResponseDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .brand(updated.getBrand())
                .status(updated.getStatus())
                .categoryName(updated.getCategory().getName())
                .build();

    }


    @Override
    public ProductResponseDTO update(long id, ProductRequestDTO dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new HttpNotFound("Product Not Found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new HttpNotFound("Category Not Found"));

        ReturnPolicy returnPolicy = returnPolicyRepository.findById(dto.getReturn_policy_id())
                .orElseThrow(() -> new HttpNotFound("Return Policy Not Found"));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        existing.setBrand(dto.getBrand());
        existing.setStatus(dto.getStatus());
        existing.setCategory(category);
        existing.setReturnPolicy(returnPolicy);

        Product updated = productRepository.save(existing);

        return ProductResponseDTO.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .brand(updated.getBrand())
                .status(updated.getStatus())
                .categoryName(updated.getCategory().getName())
                .build();
    }


    @Override
    public Page<ProductResponseDTO> pagination(Pageable pageable) {
        List<Product> products = productRepository.findAll(pageable).getContent();
        List<ProductResponseDTO> responseDTOS;
        responseDTOS = products.stream().map(product ->
                ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .status(product.getStatus())
                        .categoryName(product.getCategory().getName())
                        .build()
        ).collect(Collectors.toList());
        return new PageImpl<>(responseDTOS, pageable, responseDTOS.size());

    }

    @Override
    public List<ProductResponseDTO> search(String keyword) {
        List<Product> products = productRepository.findProductByNameContainsIgnoreCase(keyword);
        List<ProductResponseDTO> responseDTOS;
        responseDTOS = products.stream().map(product ->
                ProductResponseDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .brand(product.getBrand())
                        .status(product.getStatus())
                        .categoryName(product.getCategory().getName())
                        .build()
        ).collect(Collectors.toList());
        return responseDTOS;

    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new HttpForbiden("This product has variations you need to delete the variations first");
        }
        productRepository.deleteById(id);
    }

    @Override
    public List<Top5Product> getTop5BestSellingProducts() {
        List<Object[]> result = productRepository.findTop5BestSellingProducts(
                OrderStatus.DELIVERED,
                PageRequest.of(0, 5)
        );

        return result.stream().map(row -> {
            Product product = (Product) row[0];
            Long purchaseCount = (Long) row[1];
            Double avgRating = (Double) row[2];
            Long totalView = (Long) row[3];

            return Top5Product.builder()
                    .id(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice().doubleValue())
                    .purchaseCount(purchaseCount)
                    .averageRating(avgRating)
                    .totalReviews(totalView) // dùng lại field này cho view nếu bạn không tách riêng
                    .image(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl())
                    .build();
        }).toList();
    }

    @Override
    public List<Top5Product> getTop5LestSellingProducts() {
        List<Object[]> result = productRepository.findTop5LeastSellingWithRatingAndView(
                OrderStatus.DELIVERED,
                PageRequest.of(0, 5)
        );

        return result.stream().map(row -> {
            Product product = (Product) row[0];
            Long purchaseCount = (Long) row[1];
            Double avgRating = (Double) row[2];
            Long totalView = (Long) row[3];

            return Top5Product.builder()
                    .id(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice().doubleValue())
                    .purchaseCount(purchaseCount)
                    .averageRating(avgRating)
                    .totalReviews(totalView) // dùng lại field này cho view nếu bạn không tách riêng
                    .image(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl())
                    .build();
        }).toList();
    }

    @Override
    public ProductResponseDTO findByName(String productName) {
        // Tìm kiếm sản phẩm theo tên, không phân biệt hoa thường
        Optional<Product> productOptional = productRepository.findByNameIgnoreCase(productName);

        // Nếu không tìm thấy thì trả về null (hoặc có thể ném exception nếu muốn)
        if (productOptional.isEmpty()) {
            return null;
        }

        Product product = productOptional.get();

        // Convert Entity => DTO
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .brand(product.getBrand())
                .status(product.getStatus())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }

}
