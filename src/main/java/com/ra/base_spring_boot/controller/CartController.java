package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.ICartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/carts")
public class CartController {
    @Autowired
    private ICartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("You are not logged in");
        }
        return ResponseEntity.ok(cartService.getUserCart(userDetails.getUser().getId()));
    }


    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestBody CartItemRequestDTO request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Please login to add products to cart");
        }
        return ResponseEntity.ok(cartService.addItemToCart(userDetails.getUser().getId(), request));
    }

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<?> updateQuantity(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Please login to update quantity");
        }
        return ResponseEntity.ok(cartService.updateItemQuantity(userDetails.getUser().getId(), cartItemId, quantity));
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<?> removeItem(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @PathVariable Long cartItemId
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Please login to remove product from cart");
        }
        cartService.removeItem(userDetails.getUser().getId(), cartItemId);
        return ResponseEntity.ok("Item Removed");
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@AuthenticationPrincipal MyUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Please login to clear cart");
        }
        cartService.clearCart(userDetails.getUser().getId());
        return ResponseEntity.ok("Cart Cleared");
    }
}