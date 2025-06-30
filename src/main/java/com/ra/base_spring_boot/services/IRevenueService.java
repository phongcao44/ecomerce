package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.RevenueResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface IRevenueService {
    List<RevenueResponseDTO> getRevenueByRange(LocalDate from, LocalDate to);


    List<RevenueResponseDTO> getRevenueByDate(LocalDate date);

    List<RevenueResponseDTO> getRevenueByMonth(int month, int year);

    List<RevenueResponseDTO> getRevenueByYear(int year);

    List<RevenueResponseDTO> getRevenueByDayOnly(int day);
}
