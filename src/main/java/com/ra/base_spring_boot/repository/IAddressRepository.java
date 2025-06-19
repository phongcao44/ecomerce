package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IAddressRepository extends JpaRepository<Address,Long> {
}
