package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.AddressRequest;
import com.ra.base_spring_boot.dto.resp.AddressRespone;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IAddressRepository;
import com.ra.base_spring_boot.services.IAddressService;
import com.ra.base_spring_boot.services.IUserService;
import com.ra.base_spring_boot.services.ghn.GhnClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class AddressServiceImpl implements IAddressService {
    private final IUserService iUserService;
    private final IAddressRepository iAddressRepository;
    private final GhnClient ghnClient;

    public AddressServiceImpl(
            IAddressRepository iAddressRepository,
            IUserService iUserService,
            GhnClient ghnClient) {
        this.iUserService = iUserService;
        this.iAddressRepository = iAddressRepository;
        this.ghnClient = ghnClient;
    }



    @Override
    public void addAddress(Long userId, AddressRequest newAddress) {
        User user = iUserService.findUser(userId);
        String provineName = ghnClient.getProvinceName(newAddress.getProvinceId());
        String districtName = ghnClient.getDistrictName(newAddress.getDistrictId(), newAddress.getProvinceId());
        String wardName = ghnClient.getWardName(newAddress.getWardId(), newAddress.getDistrictId());


        Address address = Address.builder().
                user(user).
                province(provineName).
                provinceId(newAddress.getProvinceId()).
                ward(wardName).
                ward(newAddress.getWardId()).
                district(districtName).
                districtId(newAddress.getDistrictId()).
                fullAddress(newAddress.getFullAddress()).
                phone(newAddress.getPhone()).
                recipientName(newAddress.getRecipientName()).
                build();
        iAddressRepository.save(address);
    }



    @Override
    public Address findById(long id) {
        return iAddressRepository.findById(id).orElseThrow(() -> new HttpNotFound("Address not found"));
    }

    @Override
    public void delete(Long userId, Long id) {
        Address address = findById(id);
        if(address.getUser().getId().equals(userId)){
            iAddressRepository.deleteById(id);
        }
    }

    @Override
    public void update(Long userId,Long id,AddressRequest Address) {
        String provineName = ghnClient.getProvinceName(Address.getProvinceId());
        String districtName = ghnClient.getDistrictName(Address.getDistrictId(), Address.getProvinceId());
        String wardName = ghnClient.getWardName(Address.getWardId(), Address.getDistrictId());

        Address address = findById(id);
        if(address.getUser().getId().equals(userId)){
            address.setUser(iUserService.findUser(userId));
            address.setProvince(provineName);
            address.setProvinceId(Address.getProvinceId());
            address.setDistrictId(address.getDistrictId());
            address.setWard(wardName);
            address.setWardCode(address.getWardCode());
            address.setDistrict(districtName);
            address.setFullAddress(Address.getFullAddress());
            address.setPhone(Address.getPhone());
            address.setRecipientName(Address.getRecipientName());
            iAddressRepository.save(address);
        }
    }

    @Override
    public List<AddressRespone> findAllByUserId(Long userId) {
        List<Address> list =  iAddressRepository.findAllByUserId(userId);

     return list.stream().map(
             address -> AddressRespone.builder()
                     .provinceName(address.getProvince())
                     .districtName(address.getDistrict())
                     .wardName(address.getWard())
                     .recipientName(address.getRecipientName())
                     .phone(address.getPhone())
                     .fullAddress(address.getFullAddress())
                     .build()
     ).collect(Collectors.toList());
    }

}
