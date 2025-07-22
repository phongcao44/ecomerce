package com.ra.base_spring_boot.specification;

import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {
    public static Specification<User> hasRole(String role) {
        return (root, query, cb) -> cb.isMember(role, root.get("roles"));
    }

    public static Specification<User> hasRank(String rank) {
        return (root, query, cb) -> {
            if (rank == null || rank.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("userPoint").get("userRank"), rank);
        };
    }


    public static Specification<User> hasKeyword(String keyword) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), "%" + keyword.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("email")), "%" + keyword.toLowerCase() + "%")
        );
    }
}
