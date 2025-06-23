package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.WishListResponse;
import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.Wishlist;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IWishListRepository;
import com.ra.base_spring_boot.services.IUserService;
import com.ra.base_spring_boot.services.IWishListService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishListServiceImpl implements IWishListService {
    private final IUserService iUserService;
    private final IWishListRepository iWishListRepository;
    private final IProductRepository iProductRepository;
    public WishListServiceImpl(IProductRepository iProductRepository, IUserService iUserService, IWishListRepository iWishListRepository) {
        this.iUserService = iUserService;
        this.iWishListRepository = iWishListRepository;
        this.iProductRepository = iProductRepository;
    }


    @Override
    public Wishlist findByWishlistId(long userID,Long wishlistId) {
        User user = iUserService.findUser(userID);
        try{
            return iWishListRepository.findByUserIdAndProduct_Id(userID,wishlistId).orElse(null);
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public Wishlist addWishlist(long userID,long productId) {

        System.out.println(">> userID = " + userID);
        System.out.println(">> productId = " + productId);
        User user = iUserService.findUser(userID);
        Product product = iProductRepository.findById(productId).orElse(null);
        System.out.println("Product = " + product);



        if (user == null) {
            System.out.println("User not found");
            throw new RuntimeException("User not found");
        }
        if (product == null) {
            System.out.println("Product not found");
            throw new RuntimeException("Product not found");
        }

        Optional<Wishlist> existing = iWishListRepository.findByUserIdAndProduct_Id(userID, productId);
        if(existing.isPresent()){
            return existing.get();
        }
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProduct(product);
            wishlist.setCreatedAt(LocalDateTime.now());


        return iWishListRepository.save(wishlist);
    }

    @Override
    public void deleteWishlist(long userID,long wishlistId) {

    Wishlist wishlist = iWishListRepository.findByIdAndUser_Id(userID,wishlistId).orElse(null);
    iWishListRepository.delete(wishlist);
    }




    @Override
    public List<WishListResponse> findAllWishlist(long userId) {
        List<Wishlist> wishlists = iWishListRepository.findAllByUser_Id(userId);

        return wishlists.stream().map(
                wishlist -> WishListResponse.builder()
                        .productId(wishlist.getId())
                        .productName(wishlist.getProduct().getName())
                        .price(wishlist.getProduct().getPrice())
                        .userId(userId)
                        .build()
        ).collect(Collectors.toList());

    }


}
