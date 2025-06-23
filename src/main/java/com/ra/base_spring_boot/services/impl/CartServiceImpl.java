package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.resp.CartItemResponseDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;
import com.ra.base_spring_boot.exception.HttpForbiden;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.Cart;
import com.ra.base_spring_boot.model.CartItem;
import com.ra.base_spring_boot.model.ProductVariant;
import com.ra.base_spring_boot.repository.ICartItemRepository;
import com.ra.base_spring_boot.repository.ICartRepository;
import com.ra.base_spring_boot.repository.IProductVariantRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.ICartService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements ICartService {
    private final ICartRepository cartRepository;

    private final ICartItemRepository cartItemRepository;

    private final IProductVariantRepository productVariantRepository;

    private final IUserRepository userRepository;

    private Cart getOrCreateCart(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        List<Cart> carts = cartRepository.findByUser(user);
        if (!carts.isEmpty()) return carts.get(0);

        return cartRepository.save(Cart.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .build());
    }


    private CartResponseDTO buildCartResponse(Cart cart) {
        var items = cartItemRepository.findAllByCart(cart).stream().map(item -> {
            var variant = item.getVariant();
            var product = variant.getProduct();

            // Gán giá đúng: lấy priceOverride nếu có, ngược lại lấy product price
            BigDecimal price = variant.getPriceOverride() != null
                    ? variant.getPriceOverride()
                    : product.getPrice();

            return CartItemResponseDTO.builder()
                    .cartItemId(item.getId())
                    .productName(product.getName())
                    .color(variant.getColor().getName())
                    .size(variant.getSize().getSizeName())
                    .quantity(item.getQuantity())
                    .price(price)
                    .totalPrice(price.multiply(BigDecimal.valueOf(item.getQuantity()))) // ✅ dùng price vừa gán
                    .build();
        }).toList();

        if (items.isEmpty()) {
            // Nếu không có sản phẩm nào, throw ra một lỗi tùy chỉnh
            throw new HttpNotFound("There are no products in the cart.");
        }

        return CartResponseDTO.builder()
                .cartId(cart.getId())
                .items(items)
                .build();
    }



    @Override
    public CartResponseDTO getUserCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    @Override
    public CartResponseDTO addItemToCart(Long userId, CartItemRequestDTO request) {
        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new HttpNotFound("ProductVariant Not Found"));

        List<CartItem> existingItems = cartItemRepository.findByCartAndVariant(cart, variant);
        CartItem item;

        if (!existingItems.isEmpty()) {
            // Cộng tổng quantity của tất cả các dòng
            int totalQuantity = request.getQuantity();
            for (CartItem existing : existingItems) {
                totalQuantity += existing.getQuantity();
            }

            // Giữ lại dòng đầu tiên, cập nhật quantity
            item = existingItems.get(0);
            item.setQuantity(totalQuantity);

            // Xoá các dòng dư
            for (int i = 1; i < existingItems.size(); i++) {
                cartItemRepository.delete(existingItems.get(i));
            }
        } else {
            // Nếu chưa có thì tạo mới
            item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
        }

        cartItemRepository.save(item);
        return buildCartResponse(cart);
    }

    @Override
    public CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new HttpNotFound("Cart Item Not Found"));

        if (!item.getCart().getUser().getId().equals(userId))
            throw new HttpUnAuthorized("Access Denied");

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return buildCartResponse(item.getCart());
    }


    @Override
    public void removeItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new HttpNotFound("Cart Item Not Found"));
        if (!item.getCart().getUser().getId().equals(userId))
            throw new HttpUnAuthorized("Access Denied");
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCart(cart);
    }

}

