package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.dto.resp.VoucherResponse;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.Voucher;
import com.ra.base_spring_boot.model.constants.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IVoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String id);
    @Query("SELECT v FROM Voucher v WHERE v.collectible = true AND v.quantity > v.collected")
    List<Voucher> findCollectibleVouchersAvailable();
}
