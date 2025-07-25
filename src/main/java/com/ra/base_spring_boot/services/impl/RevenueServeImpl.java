package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.ProductResponseDTO;
import com.ra.base_spring_boot.dto.resp.RevenueResponseDTO;
import com.ra.base_spring_boot.dto.resp.Top5Product;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.repository.IProductRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IOrderService;
import com.ra.base_spring_boot.services.IProductService;
import com.ra.base_spring_boot.services.IRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueServeImpl implements IRevenueService {

    private final IOrderRepository orderRepository;

    private final IUserRepository userRepository;

    private final IProductRepository productRepository;

    private final IProductService productService;


    @Override
    public List<RevenueResponseDTO> getRevenueByRange(LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findAllByStatusAndCreatedAtBetween(
                OrderStatus.DELIVERED,
                from.atStartOfDay(),
                to.atTime(23, 59, 59)
        );

        Map<LocalDate, BigDecimal> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        Collectors.mapping(Order::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return grouped.entrySet().stream()
                .map(entry -> RevenueResponseDTO.builder()
                        .time(entry.getKey())
                        .totalRevenue(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(RevenueResponseDTO::getTime))
                .collect(Collectors.toList());

    }

    @Override
    public RevenueResponseDTO getDashboardStats() {
        List<Order> deliveredOrders = orderRepository.findAllByStatus(OrderStatus.DELIVERED);

        BigDecimal totalRevenue = deliveredOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = orderRepository.findAll().size();
        int totalCustomers = (int) userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ROLE_USER))
                .count();
        int totalProducts = productRepository.findAll().size();
        int totalDelivered = deliveredOrders.size();
        List<ProductResponseDTO> top5BestSellers = productService.getTop5BestSellingProducts();

        return RevenueResponseDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .totalDelivered(totalDelivered)
                .top5BestSellers(top5BestSellers)
                .build();
    }


    @Override
    public List<RevenueResponseDTO> getRevenueByDate(LocalDate date) {
        return getRevenueByRange(date, date); // chỉ một ngày
    }

    @Override
    public List<RevenueResponseDTO> getRevenueByMonth(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return getRevenueByRange(start, end);
    }

    @Override
    public List<RevenueResponseDTO> getRevenueByYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getRevenueByRange(start, end);
    }

    @Override
    public List<RevenueResponseDTO> getRevenueByDayOnly(int day) {
        List<Order> orders = orderRepository.findAllByStatus(OrderStatus.DELIVERED);

        Map<LocalDate, BigDecimal> grouped = orders.stream()
                .filter(order -> order.getCreatedAt().getDayOfMonth() == day)
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        Collectors.mapping(Order::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return grouped.entrySet().stream()
                .map(entry -> RevenueResponseDTO.builder()
                        .time(entry.getKey())
                        .totalRevenue(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(RevenueResponseDTO::getTime))
                .collect(Collectors.toList());

    }
}
