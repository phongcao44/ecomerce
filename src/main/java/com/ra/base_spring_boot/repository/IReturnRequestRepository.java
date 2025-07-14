package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByUserId(Long userId);
    @Query("SELECT rr FROM ReturnRequest rr " +
            "JOIN rr.order o " +
            "JOIN o.orderItems oi " +
            "WHERE o.id = :orderId AND oi.variant.id = :variantId")
    Optional<ReturnRequest> findByOrderIdAndProductVariantViaOrderItems(Long orderId, Long variantId);

    Optional<ReturnRequest> findByOrder_IdAndProductVariant_Id(Long orderId, Long variantId);


}
