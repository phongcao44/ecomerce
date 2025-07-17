package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.ProductCompareResponse;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.CompareItem;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductSpecification;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.CompareItemRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.ProductSpecificationRepository;
import com.ra.base_spring_boot.services.ICompareService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CompareServiceImpl implements ICompareService {

    private final CompareItemRepository compareItemRepository;

    private final IProductRepository productRepository;

    private final ProductSpecificationRepository specificationRepository;

    private final IUserRepository userRepository;

    @Override
    public String addToCompare(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new HttpNotFound("Product Not Found"));

        boolean exists = compareItemRepository.existsByUserAndProduct(user, product);
        if (exists) {
            throw new HttpBadRequest("Sản phẩm đã có trong danh sách so sánh");
        }

        CompareItem compareItem = CompareItem.builder()
                .user(user)
                .product(product)
                .addedAt(LocalDateTime.now())
                .build();

        compareItemRepository.save(compareItem);
        return "Đã thêm vào danh sách so sánh";
    }


    @Override
    @Transactional
    public String removeFromCompare(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new HttpNotFound("Product Not Found"));

        compareItemRepository.deleteByUserAndProduct(user, product);
        return "Đã xoá khỏi danh sách so sánh";
    }

    @Override
    public List<ProductCompareResponse> getCompareList(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new HttpNotFound("User Not Found"));

//        List<CompareItem> items = compareItemRepository.findAllByUser(user);
//        if (items.isEmpty()) return new ArrayList<>();
//
//        List<ProductCompareResponse> responses = new ArrayList<>();
//
//        for (CompareItem item : items) {
//            Product product = item.getProduct();
//            List<ProductSpecification> specs = specificationRepository.findAllByProduct(product);
//
//            Map<String, String> specMap = new LinkedHashMap<>();
//            for (ProductSpecification spec : specs) {
//                specMap.put(spec.getSpecKey(), spec.getSpecValue());
//            }
//
//            ProductCompareResponse response = ProductCompareResponse.builder()
//                    .productId(product.getId())
//                    .productName(product.getName())
//                    .specifications(specMap)
//                    .build();
//
//            responses.add(response);
//        }
//
//        return responses;
//    }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        List<CompareItem> items = compareItemRepository.findAllByUser(user);
        if (items.isEmpty()) return new ArrayList<>();

        Map<String, ProductCompareResponse> map = new LinkedHashMap<>();

        for (CompareItem item : items) {
            Product product = item.getProduct();
            List<ProductSpecification> specs = specificationRepository.findAllByProduct(product);
            for (ProductSpecification spec : specs) {
                map.putIfAbsent(spec.getSpecKey(),
                        ProductCompareResponse.builder()
                                .specificationName(spec.getSpecKey())
                                .productValues(new HashMap<>())
                                .build());

                map.get(spec.getSpecKey()).getProductValues()
                        .put(product.getName(), spec.getSpecValue());
            }
        }

        return new ArrayList<>(map.values());
    }
}