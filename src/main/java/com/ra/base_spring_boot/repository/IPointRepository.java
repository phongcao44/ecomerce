package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;


public interface IPointRepository extends JpaRepository<UserPoint,Long> {

    UserPoint findByUserId(Long userId);
}
