package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.RevenueResponseDTO;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import com.ra.base_spring_boot.repository.IOrderRepository;
import com.ra.base_spring_boot.services.IOrderService;
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

    @Autowired
    private IOrderRepository orderRepository;

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
                .map(entry -> new RevenueResponseDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RevenueResponseDTO::getTime))
                .collect(Collectors.toList());
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
                .map(entry -> new RevenueResponseDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RevenueResponseDTO::getTime))
                .collect(Collectors.toList());
    }
}
