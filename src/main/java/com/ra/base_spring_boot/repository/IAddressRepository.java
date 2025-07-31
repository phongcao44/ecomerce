package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IAddressRepository extends JpaRepository<Address,Long> {
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.deleted = false")
    List<Address> findAllByUserId(@Param("userId") Long userId);

    Optional<Address> findByUserId(Long userId);

    Long user(User user);

    @Query("SELECT o.shippingAddress FROM Order o WHERE o.id = :orderId")
    Address findShippingAddressByOrderId(@Param("orderId") Long orderId);
}
