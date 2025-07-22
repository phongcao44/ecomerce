package com.ra.base_spring_boot.specifications;

import com.ra.base_spring_boot.model.Order;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecifications {
    public static Specification<Order> hasStatus(String status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Order> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(root.get("id").as(String.class), "%" + keyword + "%"),
                        criteriaBuilder.like(root.get("user").get("username"), "%" + keyword + "%")
                );
    }

    public static Specification<Order> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
}
