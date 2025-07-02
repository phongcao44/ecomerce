package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IAddressRepository extends JpaRepository<Address,Long> {
    List<Address> findAllByUserId(Long userId);

    Long user(User user);
}
