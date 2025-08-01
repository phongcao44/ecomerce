package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.WishlistItemResponseDTO;
import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.WishListResponse;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.Wishlist;

import java.awt.print.Pageable;
import java.util.List;

public interface IWishListService {
    Wishlist  findByWishlistId(long userID,Long wishlistId);
    Wishlist addWishlist(long userID, long productId);
    void deleteWishlist(long userID,long wishlist);
    List<WishlistItemResponseDTO> findAllWishlist(long userID);

}
