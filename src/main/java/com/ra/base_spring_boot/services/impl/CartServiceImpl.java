package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.CartItemRequestDTO;
import com.ra.base_spring_boot.dto.req.OrderRequestDTO;
import com.ra.base_spring_boot.dto.resp.CartItemResponseDTO;
import com.ra.base_spring_boot.dto.resp.CartResponseDTO;
import com.ra.base_spring_boot.dto.resp.OrderCheckoutResponseDTO;
import com.ra.base_spring_boot.dto.resp.OrderItemDetailDTO;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.model.*;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.services.*;
import com.ra.base_spring_boot.model.constants.DiscountType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final ShippingFeeService shippingFeeService;

    private final ShippingFeeRepository shippingFeeRepository;

    private final IVoucherRepository voucherRepository;

    private final IPointRepository pointRepository;

    private final IFlashSaleItemRepository flashSaleItemRepository;

    private final IPointService iPointService;

    private final IFlashSaleItemService flashSaleItemService;



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

            BigDecimal originalPrice = variant.getPriceOverride() != null
                    ? variant.getPriceOverride()
                    : product.getPrice();

            BigDecimal discountedPrice = originalPrice;
            BigDecimal discountAmount = BigDecimal.ZERO;
            String discountType = null;
            BigDecimal discountOverrideByFlashSale = BigDecimal.ZERO;

            FlashSaleItem flashItem = flashSaleItemRepository.findByVariant(variant);
            if (flashItem != null && flashItem.getFlashSale().getStatus() == UserStatus.ACTIVE) {
                DiscountType type = flashItem.getDiscountType();
                BigDecimal value = flashItem.getDiscountedPrice();

                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    discountOverrideByFlashSale = value;

                    if (type == DiscountType.PERCENTAGE) {
                        discountAmount = originalPrice.multiply(value)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        discountedPrice = originalPrice.subtract(discountAmount);
                    } else if (type == DiscountType.AMOUNT) {
                        discountAmount = value;
                        discountedPrice = originalPrice.subtract(discountAmount).max(BigDecimal.ZERO);
                    }

                    discountType = type.name();
                }
            }

                return CartItemResponseDTO.builder()
                    .cartItemId(item.getId())
                    .productName(product.getName())
                    .color(variant.getColor().getName())
                    .size(variant.getSize().getSizeName())
                    .quantity(item.getQuantity())
                    .originalPrice(originalPrice)
                    .discountedPrice(discountedPrice)
                    .discountAmount(discountAmount)
                    .discountType(discountType)
                    .discountOverrideByFlashSale(discountOverrideByFlashSale)
                    .totalPrice(discountedPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new HttpNotFound("Shipping Address Not Found"));

        List<CartItem> selectedItems = cartItemRepository.findAllById(request.getCartItemIds());
        if (selectedItems.isEmpty()) {
            throw new HttpNotFound("No selected cart items found.");
        }

        for (CartItem item : selectedItems) {
            if (!item.getCart().getUser().getId().equals(userId)) {
                throw new HttpUnAuthorized("Access Denied");
            }
        }

        ShippingFee shippingFee = shippingFeeService.calculateAndSaveShippingFee(userId, address);

        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .shippingFee(shippingFee)
                .build();
        orderRepository.save(order);

        shippingFee.setOrder(order);
        shippingFeeRepository.save(shippingFee);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : selectedItems) {
            ProductVariant variant = cartItem.getVariant();
            int quantity = cartItem.getQuantity();

            BigDecimal basePrice = variant.getPriceOverride() != null
                    ? variant.getPriceOverride()
                    : variant.getProduct().getPrice();

            BigDecimal finalPrice = basePrice;

            FlashSaleItem flashItem = flashSaleItemRepository.findByVariant(variant);
            if (flashItem != null) {
                FlashSale flashSale = flashItem.getFlashSale();

                if (flashSale != null && flashSale.getStatus() == UserStatus.ACTIVE) {
                    Integer limit = flashItem.getQuantityLimit();
                    Integer sold = flashItem.getSoldQuantity() != null ? flashItem.getSoldQuantity() : 0;

                    if (limit == null || sold + quantity <= limit) {
                        DiscountType type = flashItem.getDiscountType();
                        BigDecimal discountValue = flashItem.getDiscountedPrice(); // dùng chung

                        if (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0) {
                            if (type == DiscountType.PERCENTAGE) {
                                // giảm phần trăm
                                finalPrice = basePrice.multiply(BigDecimal.valueOf(100).subtract(discountValue))
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            } else if (type == DiscountType.AMOUNT) {
                                // giảm số tiền trực tiếp
                                finalPrice = basePrice.subtract(discountValue).max(BigDecimal.ZERO);
                            }
                        }

                        // Cập nhật sold_quantity
                        flashItem.setSoldQuantity(sold + quantity);
                        flashSaleItemService.save(flashItem);
                    }
                }
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .quantity(quantity)
                    .priceAtTime(finalPrice)
                    .build();
            orderItemRepository.save(orderItem);

            total = total.add(finalPrice.multiply(BigDecimal.valueOf(quantity)));
            orderItems.add(orderItem);
        }

        // Optional voucher
        if (request.getVoucherId() != null && request.getVoucherId() > 0) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new HttpNotFound("Voucher not found"));

            if (voucher.getQuantity() <= 0) {
                throw new HttpBadRequest("Voucher has been fully used.");
            }

            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);

            order.setVoucher(voucher);

            if (voucher.getDiscountAmount() != null) {
                BigDecimal amount = BigDecimal.valueOf(voucher.getDiscountAmount());
                order.setDiscountAmount(amount.doubleValue());
                total = total.subtract(amount);
            } else if (voucher.getDiscountPercent() != null) {
                BigDecimal percent = BigDecimal.valueOf(voucher.getDiscountPercent());
                BigDecimal discount = total.multiply(percent).divide(BigDecimal.valueOf(100));
                order.setDiscountPercent(percent.doubleValue());
                total = total.subtract(discount);
            }
        }

        // Áp dụng điểm người dùng
        int used = request.getUsedPoints() != null ? request.getUsedPoints() : 0;
        if (used > 0) {
            UserPoint userPoint = pointRepository.findByUser(user);
            int currentPoints = userPoint.getTotalPoints();

            if (used > currentPoints) {
                throw new IllegalArgumentException("Bạn không có đủ điểm để sử dụng");
            }

            // Trừ điểm
            userPoint.setTotalPoints(currentPoints - used);
            pointRepository.save(userPoint);

            // Ghi lại vào đơn hàng
            order.setUsedPoints(used);
            total = total.subtract(BigDecimal.valueOf(used));
        }


        // Add shipping fee
        total = total.add(BigDecimal.valueOf(shippingFee.getTotal()));
        order.setTotalAmount(total);
        orderRepository.save(order);

        // Cộng điểm thưởng
        iPointService.accumulatePoints(order);

        // Xoá cart item đã thanh toán
        cartItemRepository.deleteAll(selectedItems);

        return OrderCheckoutResponseDTO.fromOrder(order, orderItems);
    }

    @Override
    @Transactional
    public OrderCheckoutResponseDTO checkoutSelectedItems(Long userId, OrderRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new HttpNotFound("Shipping Address Not Found"));

        List<CartItem> selectedItems = cartItemRepository.findAllById(request.getCartItemIds());
        if (selectedItems.isEmpty()) {
            throw new HttpNotFound("No selected cart items found.");
        }

        for (CartItem item : selectedItems) {
            if (!item.getCart().getUser().getId().equals(userId)) {
                throw new HttpUnAuthorized("Access Denied");
            }
        }

        ShippingFee shippingFee = shippingFeeService.calculateAndSaveShippingFee(userId, address);

        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .shippingFee(shippingFee)
                .build();
        orderRepository.save(order);

        shippingFee.setOrder(order);
        shippingFeeRepository.save(shippingFee);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : selectedItems) {
            ProductVariant variant = cartItem.getVariant();
            int quantity = cartItem.getQuantity();

            BigDecimal basePrice = variant.getPriceOverride() != null
                    ? variant.getPriceOverride()
                    : variant.getProduct().getPrice();

            BigDecimal finalPrice = basePrice;

            FlashSaleItem flashItem = flashSaleItemRepository.findByVariant(variant);
            if (flashItem != null) {
                FlashSale flashSale = flashItem.getFlashSale();

                if (flashSale != null && flashSale.getStatus() == UserStatus.ACTIVE) {
                    Integer limit = flashItem.getQuantityLimit();
                    Integer sold = flashItem.getSoldQuantity() != null ? flashItem.getSoldQuantity() : 0;

                    if (limit == null || sold + quantity <= limit) {
                        DiscountType type = flashItem.getDiscountType();
                        BigDecimal discountValue = flashItem.getDiscountedPrice(); // dùng chung

                        if (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0) {
                            if (type == DiscountType.PERCENTAGE) {
                                // giảm phần trăm
                                finalPrice = basePrice.multiply(BigDecimal.valueOf(100).subtract(discountValue))
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            } else if (type == DiscountType.AMOUNT) {
                                // giảm số tiền trực tiếp
                                finalPrice = basePrice.subtract(discountValue).max(BigDecimal.ZERO);
                            }
                        }

                        // Cập nhật sold_quantity
                        flashItem.setSoldQuantity(sold + quantity);
                        flashSaleItemService.save(flashItem);
                    }
                }
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .quantity(quantity)
                    .priceAtTime(finalPrice)
                    .build();
            orderItemRepository.save(orderItem);

            total = total.add(finalPrice.multiply(BigDecimal.valueOf(quantity)));
            orderItems.add(orderItem);
        }

        // Optional voucher
        if (request.getVoucherId() != null && request.getVoucherId() > 0) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new HttpNotFound("Voucher not found"));

            if (voucher.getQuantity() <= 0) {
                throw new HttpBadRequest("Voucher has been fully used.");
            }

            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);

            order.setVoucher(voucher);

            if (voucher.getDiscountAmount() != null) {
                BigDecimal amount = BigDecimal.valueOf(voucher.getDiscountAmount());
                order.setDiscountAmount(amount.doubleValue());
                total = total.subtract(amount);
            } else if (voucher.getDiscountPercent() != null) {
                BigDecimal percent = BigDecimal.valueOf(voucher.getDiscountPercent());
                BigDecimal discount = total.multiply(percent).divide(BigDecimal.valueOf(100));
                order.setDiscountPercent(percent.doubleValue());
                total = total.subtract(discount);
            }
        }

        // Áp dụng điểm người dùng
        int used = request.getUsedPoints() != null ? request.getUsedPoints() : 0;
        if (used > 0) {
            UserPoint userPoint = pointRepository.findByUser(user);
            int currentPoints = userPoint.getTotalPoints();

            if (used > currentPoints) {
                throw new IllegalArgumentException("Bạn không có đủ điểm để sử dụng");
            }

            // Trừ điểm
            userPoint.setTotalPoints(currentPoints - used);
            pointRepository.save(userPoint);

            // Ghi lại vào đơn hàng
            order.setUsedPoints(used);
            total = total.subtract(BigDecimal.valueOf(used));
        }


        // Add shipping fee
        total = total.add(BigDecimal.valueOf(shippingFee.getTotal()));
        order.setTotalAmount(total);
        orderRepository.save(order);

        // Cộng điểm thưởng
        iPointService.accumulatePoints(order);

        // Xoá cart item đã thanh toán
        cartItemRepository.deleteAll(selectedItems);

        return OrderCheckoutResponseDTO.fromOrder(order, orderItems);
    }



    @Override
    public OrderCheckoutResponseDTO checkoutByCartItemId(Long userId, Long cartItemId, OrderRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User Not Found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new HttpNotFound("Shipping Address Not Found"));

        List<CartItem> selectedItems = cartItemRepository.findAllById(request.getCartItemIds());
        if (selectedItems.isEmpty()) {
            throw new HttpNotFound("No selected cart items found.");
        }

        for (CartItem item : selectedItems) {
            if (!item.getCart().getUser().getId().equals(userId)) {
                throw new HttpUnAuthorized("Access Denied");
            }
        }

        ShippingFee shippingFee = shippingFeeService.calculateAndSaveShippingFee(userId, address);

        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .shippingFee(shippingFee)
                .build();
        orderRepository.save(order);

        shippingFee.setOrder(order);
        shippingFeeRepository.save(shippingFee);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : selectedItems) {
            ProductVariant variant = cartItem.getVariant();
            int quantity = cartItem.getQuantity();

            BigDecimal basePrice = variant.getPriceOverride() != null
                    ? variant.getPriceOverride()
                    : variant.getProduct().getPrice();

            BigDecimal finalPrice = basePrice;

            FlashSaleItem flashItem = flashSaleItemRepository.findByVariant(variant);
            if (flashItem != null) {
                FlashSale flashSale = flashItem.getFlashSale();

                if (flashSale != null && flashSale.getStatus() == UserStatus.ACTIVE) {
                    Integer limit = flashItem.getQuantityLimit();
                    Integer sold = flashItem.getSoldQuantity() != null ? flashItem.getSoldQuantity() : 0;

                    if (limit == null || sold + quantity <= limit) {
                        DiscountType type = flashItem.getDiscountType();
                        BigDecimal discountValue = flashItem.getDiscountedPrice(); // dùng chung

                        if (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0) {
                            if (type == DiscountType.PERCENTAGE) {
                                // giảm phần trăm
                                finalPrice = basePrice.multiply(BigDecimal.valueOf(100).subtract(discountValue))
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            } else if (type == DiscountType.AMOUNT) {
                                // giảm số tiền trực tiếp
                                finalPrice = basePrice.subtract(discountValue).max(BigDecimal.ZERO);
                            }
                        }

                        // Cập nhật sold_quantity
                        flashItem.setSoldQuantity(sold + quantity);
                        flashSaleItemService.save(flashItem);
                    }
                }
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .quantity(quantity)
                    .priceAtTime(finalPrice)
                    .build();
            orderItemRepository.save(orderItem);

            total = total.add(finalPrice.multiply(BigDecimal.valueOf(quantity)));
            orderItems.add(orderItem);
        }

        // Optional voucher
        if (request.getVoucherId() != null && request.getVoucherId() > 0) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new HttpNotFound("Voucher not found"));

            if (voucher.getQuantity() <= 0) {
                throw new HttpBadRequest("Voucher has been fully used.");
            }

            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);

            order.setVoucher(voucher);

            if (voucher.getDiscountAmount() != null) {
                BigDecimal amount = BigDecimal.valueOf(voucher.getDiscountAmount());
                order.setDiscountAmount(amount.doubleValue());
                total = total.subtract(amount);
            } else if (voucher.getDiscountPercent() != null) {
                BigDecimal percent = BigDecimal.valueOf(voucher.getDiscountPercent());
                BigDecimal discount = total.multiply(percent).divide(BigDecimal.valueOf(100));
                order.setDiscountPercent(percent.doubleValue());
                total = total.subtract(discount);
            }
        }

        // Áp dụng điểm người dùng
        int used = request.getUsedPoints() != null ? request.getUsedPoints() : 0;
        if (used > 0) {
            UserPoint userPoint = pointRepository.findByUser(user);
            int currentPoints = userPoint.getTotalPoints();

            if (used > currentPoints) {
                throw new IllegalArgumentException("Bạn không có đủ điểm để sử dụng");
            }

            // Trừ điểm
            userPoint.setTotalPoints(currentPoints - used);
            pointRepository.save(userPoint);

            // Ghi lại vào đơn hàng
            order.setUsedPoints(used);
            total = total.subtract(BigDecimal.valueOf(used));
        }


        // Add shipping fee
        total = total.add(BigDecimal.valueOf(shippingFee.getTotal()));
        order.setTotalAmount(total);
        orderRepository.save(order);

        // Cộng điểm thưởng
        iPointService.accumulatePoints(order);

        // Xoá cart item đã thanh toán
        cartItemRepository.deleteAll(selectedItems);

        return OrderCheckoutResponseDTO.fromOrder(order, orderItems);
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
