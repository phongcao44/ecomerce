package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.AddressRequest;
import com.ra.base_spring_boot.dto.resp.ProvineResponse;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.User;

import java.util.List;

public interface IAddressService {
    void addAddress(Long userId ,AddressRequest address);
    Address findById(long id);
    void delete(Long userId, Long id);
    void update(Long userId,Long Id,AddressRequest address);
}
