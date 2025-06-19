package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.AddressRequest;
import com.ra.base_spring_boot.dto.resp.ProvineResponse;
import com.ra.base_spring_boot.model.Address;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IAddressRepository;
import com.ra.base_spring_boot.services.IAddressService;
import com.ra.base_spring_boot.services.IUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class AddressServiceImpl implements IAddressService {
    private final IUserService iUserService;
    private final IAddressRepository iAddressRepository;
    public AddressServiceImpl(IAddressRepository iAddressRepository, IUserService iUserService) {
        this.iUserService = iUserService;
        this.iAddressRepository = iAddressRepository;
    }



    @Override
    public void addAddress(Long userId, AddressRequest newAddress) {
        User user = iUserService.findUser(userId);
        Address address = Address.builder().
                user(user).
                province(newAddress.getProvince()).
                ward(newAddress.getWard()).
                district(newAddress.getDistrict()).
                fullAddress(newAddress.getFullAddress()).
                phone(newAddress.getPhone()).
                recipientName(newAddress.getRecipientName()).
                build();
        iAddressRepository.save(address);
    }



    @Override
    public Address findById(long id) {
        return iAddressRepository.findById(id).orElse(null);
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

        Address address = findById(id);
        if(address.getUser().getId().equals(userId)){
            address.setUser(iUserService.findUser(userId));
            address.setProvince(Address.getProvince());
            address.setWard(Address.getWard());
            address.setDistrict(Address.getDistrict());
            address.setFullAddress(Address.getFullAddress());
            address.setPhone(Address.getPhone());
            address.setRecipientName(Address.getRecipientName());
            iAddressRepository.save(address);
        }
    }
}
