package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.req.OrderRequestDTO;
import com.ra.base_spring_boot.dto.resp.CartItemResponseDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;
import com.ra.base_spring_boot.dto.resp.OrderCheckoutResponseDTO;
import com.ra.base_spring_boot.dto.resp.OrderItemDetailDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.services.ICartService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements ICartService {
    private final ICartRepository cartRepository;

    private final ICartItemRepository cartItemRepository;

    private final IProductVariantRepository productVariantRepository;

    private final IUserRepository userRepository;

    private final IOrderRepository orderRepository;

    private final IAddressRepository addressRepository;

    private final IOrderItemRepository orderItemRepository;


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
                    .totalPrice(price.multiply(BigDecimal.valueOf(item.getQuantity()))) // tổng giá tiền = price * quantity
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

    @Override
    public OrderCheckoutResponseDTO checkout(Long userId, OrderRequestDTO request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        var address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new HttpNotFound("Shipping Address Not Found"));

        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findAllByCart(cart);

        if (items.isEmpty()) {
            throw new HttpNotFound("Cart Is Empty");
        }

        Order order = Order.builder()
                .user(user)
                .paymentMethod(request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .shippingAddress(address)
                .build();
        orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        OrderItem orderItem = null;
        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            Integer qty = item.getQuantity();
            BigDecimal price = variant.getPriceOverride(); // trường họp not null
//          BigDecimal price = variant.getPriceOverride() != null ? variant.getPriceOverride() : variant.getPriceOverride();

            orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .quantity(qty)
                    .priceAtTime(price)
                    .build();
            orderItemRepository.save(orderItem);

            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
            orderItems.add(orderItem);
        }

        order.setTotalAmount(total);
        orderRepository.save(order);

        cartItemRepository.deleteAll(items);

        return OrderCheckoutResponseDTO.fromOrder(order, orderItems);
    }

    @Override
    public OrderCheckoutResponseDTO checkoutByCartItemId(Long userId, Long cartItemId, OrderRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new HttpNotFound("Shipping Address Not Found"));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new HttpNotFound("Cart Item Not Found"));

        // Kiểm tra quyền
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new HttpUnAuthorized("Access Denied");
        }

        // Tạo order
        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .paymentMethod(request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();
        orderRepository.save(order);

        // Tạo order item
        ProductVariant variant = item.getVariant();
        BigDecimal price = variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : variant.getProduct().getPrice();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .variant(variant)
                .quantity(item.getQuantity())
                .priceAtTime(price)
                .build();
        orderItemRepository.save(orderItem);

        order.setTotalAmount(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        orderRepository.save(order);

        // Xoá cart item
        cartItemRepository.delete(item);

        return OrderCheckoutResponseDTO.fromOrder(order, List.of(orderItem));
    }
    @Override
    public BigDecimal getCartTotal(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findAllByCart(cart);

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getPriceOverride() != null
                    ? variant.getPriceOverride()
                    : variant.getProduct().getPrice();

            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        return total;
    }

}