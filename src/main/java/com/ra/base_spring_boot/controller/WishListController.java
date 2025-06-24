package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.DataError;
import com.ra.base_spring_boot.dto.req.WishListRequest;
import com.ra.base_spring_boot.model.Wishlist;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IWishListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user/wishList")
public class WishListController {
    private final IWishListService iWishListService;
    public WishListController(IWishListService iWishListService) {
        this.iWishListService = iWishListService;
    }

    @GetMapping
    public ResponseEntity<?> getWishList(@AuthenticationPrincipal MyUserDetails user){

        return new ResponseEntity<>(iWishListService.findAllWishlist(user.getUser().getId()), HttpStatus.OK);
    }

        @PostMapping("/add")
        public ResponseEntity<?> addWishList(@RequestBody WishListRequest productId, @AuthenticationPrincipal MyUserDetails user){
            try{
            iWishListService.addWishlist(user.getUser().getId(),productId.getProductId());
              return new ResponseEntity<>(HttpStatus.CREATED);
            } catch (Exception e) {
                e.printStackTrace(); // log lỗi chi tiết
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new DataError("Error: " + e.getMessage(), 500));
            }

        }

    @DeleteMapping
    public ResponseEntity<?> deleteWishList(@AuthenticationPrincipal MyUserDetails user, long wishlistId){
        iWishListService.deleteWishlist( wishlistId,user.getUser().getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
