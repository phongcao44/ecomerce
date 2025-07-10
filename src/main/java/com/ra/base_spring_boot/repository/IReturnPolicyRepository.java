package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.ReturnPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IReturnPolicyRepository extends JpaRepository<ReturnPolicy, Long> {
}

