package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.UserVoucher;
import com.ra.base_spring_boot.model.Voucher;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface IUserVoucherRepository extends CrudRepository<UserVoucher, Long> {
    boolean existsByUserAndVoucher(User user, Voucher voucher);
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
    Optional<UserVoucher> findByUserAndVoucher(User user, Voucher voucher);

    List<UserVoucher> findAllByUser(User user);
}
