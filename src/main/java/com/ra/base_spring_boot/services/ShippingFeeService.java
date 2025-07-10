package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.Order;
import com.ra.base_spring_boot.model.ShippingFee;

public interface ShippingFeeService {
    ShippingFee calculateAndSaveShippingFee(Long userId, Address address);
}
