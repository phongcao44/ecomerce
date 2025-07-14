package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.resp.UserPointResponse;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserPoint;
import com.ra.base_spring_boot.model.constants.UserRank;
import com.ra.base_spring_boot.repository.IPointRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IPointService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.List;

@Service
public class PointServiceImpl implements IPointService {
    private final IPointRepository pointRepository;
    private final IUserRepository userRepository;
    public PointServiceImpl(IPointRepository pointRepository, IUserRepository userRepository)
    {
        this.pointRepository = pointRepository;
        this.userRepository = userRepository;
    }


    @Override

    public void accumulatePoints(Order order) {
        User user = order.getUser();
        UserPoint userPoint = pointRepository.findByUserId(user.getId());
        BigDecimal rate = getRateByRank(userPoint.getUserRank());
        // Cộng điểm dựa trên số tiền thật sự đã chi (bao gồm cả điểm đã dùng)
        BigDecimal orderTotal = order.getTotalAmount().add(BigDecimal
                .valueOf(order.getUsedPoints() != null ? order.getUsedPoints() : 0));
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
        if (rankPoints >= 4000) return UserRank.KIMCUONG;
        else if (rankPoints >= 1500) return UserRank.VANG;
        else if (rankPoints >= 500) return UserRank.BAC;
        else return UserRank.DONG;
    }



    @Override
    public UserPointResponse getUserPoints(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        System.out.println(userId+"...............");
        UserPoint userPoint = pointRepository.findByUserId(userId);


        UserPointResponse userPointResponse = new UserPointResponse();
        userPointResponse.setUserId(userId);
        userPointResponse.setUserPoints(userPoint.getTotalPoints());
        userPointResponse.setUserRank(userPoint.getUserRank());
        userPointResponse.setRankPoints(userPoint.getRankPoints());
        return userPointResponse;
    }
    @Override
    public void SetUserPoints(User user) {
    UserPoint  userPoint = new UserPoint();
    userPoint.setUser(user);
    userPoint.setUserRank(UserRank.DONG);
    userPoint.setTotalPoints(0);
    userPoint.setRankPoints(0);
    pointRepository.save(userPoint);
    }
    @Override
    public List<UserPointResponse> getAllUserRank(Long orderId) {
        return List.of();
    }

}

