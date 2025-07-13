package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.Order;

public interface IPointService {
    void accumulatePoints(Order order);

}
