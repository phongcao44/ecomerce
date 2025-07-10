package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserPoint;
import com.ra.base_spring_boot.model.constants.UserRank;
import com.ra.base_spring_boot.repository.IPointRepository;
import com.ra.base_spring_boot.services.IPointService;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PointServiceImpl implements IPointService {
    private final IPointRepository pointRepository;
    public PointServiceImpl(IPointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }
    public void accumulatePoints(Order order) {
        User user = order.getUser();
        UserPoint userPoint = pointRepository.findByUser(user);
        BigDecimal rate = getRateByRank(userPoint.getUserRank());
        BigDecimal orderTotal = order.getTotalAmount();
        BigDecimal points = orderTotal.multiply(rate).divide(BigDecimal.valueOf(1000), RoundingMode.DOWN);
        int earnedPoints = points.intValue();
        userPoint.setTotalPoints(userPoint.getTotalPoints() + earnedPoints);
        userPoint.setRankPoints(userPoint.getRankPoints() + earnedPoints);
        userPoint.setUserRank(calculateNewRank(userPoint.getRankPoints()));

        pointRepository.save(userPoint);
    }

    private BigDecimal getRateByRank(UserRank rank) {
        return switch (rank) {
            case DONG -> BigDecimal.valueOf(1.0);
            case BAC -> BigDecimal.valueOf(1.2);
            case VANG -> BigDecimal.valueOf(1.5);
            case KIMCUONG -> BigDecimal.valueOf(2.0);
        };
    }

    private UserRank calculateNewRank(int rankPoints) {
        if (rankPoints >= 2000) return UserRank.KIMCUONG;
        else if (rankPoints >= 1000) return UserRank.VANG;
        else if (rankPoints >= 500) return UserRank.BAC;
        else return UserRank.DONG;
    }
}

