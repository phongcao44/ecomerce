package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.Product;
import com.ra.base_spring_boot.model.ProductView;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IProductViewRepository;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IProductViewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ProductViewServiceImpl implements IProductViewService {
    @Autowired
    private IProductViewRepository productViewRepository;
        @Override
        public void trackProductView(Long productId, HttpServletRequest request) {
            String sessionId = request.getSession().getId();
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            User user = getCurrentUser(); // trả về null nếu chưa login
            Optional<ProductView> recentView;

            if (user != null && user.getId() != null) {
                // Nếu đã login → kiểm tra theo userId
                recentView = productViewRepository
                        .findTopByProduct_IdAndUser_IdOrderByViewedAtDesc(productId, user.getId());
            } else {
                // Nếu chưa login → kiểm tra theo sessionId
                recentView = productViewRepository
                        .findTopByProduct_IdAndSessionIdOrderByViewedAtDesc(productId, sessionId);
            }

            boolean shouldLog = recentView.isEmpty() ||
                    recentView.get().getViewedAt().isBefore(LocalDateTime.now().minus(1, ChronoUnit.MINUTES));

            if (shouldLog) {
                ProductView view = ProductView.builder()
                        .product(Product.builder().id(productId).build())
                        .user(getCurrentUser()) // Trả về null nếu chưa đăng nhập
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .sessionId(sessionId)
                        .viewedAt(LocalDateTime.now())
                        .build();

                productViewRepository.save(view);
            }
        }
        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            return (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) ?
                    ip : request.getRemoteAddr();
        }

    //  user đang đăng nhập
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof MyUserDetails) {

            MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId(); // ← lấy id từ User bên trong

            return User.builder().id(userId).build(); // tạo User object từ ID
        }
        return null;
    }
}