package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.resp.AddressRespone;

import java.util.List;

public interface IAddressLookupService {
    List<AddressRespone> findAllByUserId(Long userId);
}
