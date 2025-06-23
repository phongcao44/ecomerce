package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IWishListRepository extends JpaRepository<Wishlist,Long> {
    Optional<Wishlist> findByUserIdAndProduct_Id(Long userId, Long productId);
    Optional<Wishlist> findByIdAndUser_Id(Long wishlistId, Long userId);
    List<Wishlist> findAllByUser_Id(Long userId);
}
