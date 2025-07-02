package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.AddressRequest;
import com.ra.base_spring_boot.dto.resp.AddressRespone;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.ghn.GhnClient;
import com.ra.base_spring_boot.services.impl.AddressServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AddressController {
    private final AddressServiceImpl addressService;
    private final GhnClient ghnClient;

    @GetMapping("/user/provinces")
    public ResponseEntity<String> getProvinces() {
        return ghnClient.getProvinces();
    }

    @GetMapping("/user/districts")
    public ResponseEntity<String> getDistricts(@RequestParam("province_id") Integer provinceId) {
        return ghnClient.getDistricts(provinceId);
    }

    @GetMapping("/user/wards")
    public ResponseEntity<String> getWards(@RequestParam("district_id") Integer districtId) {
        return ghnClient.getWards(districtId);
    }

    @PostMapping("/user/address/add")
    public ResponseEntity<?> addAddress(
            @RequestBody AddressRequest addressRequest,
            @AuthenticationPrincipal MyUserDetails user) {
        addressService.addAddress(user.getUser().getId(),addressRequest);
                return ResponseEntity.ok().build();
    }
    @DeleteMapping("/user/address/delete")
    public ResponseEntity<?> deleteAddress(
            @AuthenticationPrincipal MyUserDetails user,
            @RequestBody long addressId
    ){
        addressService.delete(user.getUser().getId(),addressId);
        return ResponseEntity.ok().build();
    }
    @PatchMapping("/user/address/update")
    public ResponseEntity<?> updateAddress(
            @RequestBody AddressRequest addressRequest,
            @AuthenticationPrincipal MyUserDetails user,
            long addressId
    ){
        addressService.update(user.getUser().getId(), addressId, addressRequest);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/user/address/All")
    public ResponseEntity<List<?>> getAllAddress(@AuthenticationPrincipal MyUserDetails user){
        List<AddressRespone> respones = addressService.findAllByUserId(user.getUser().getId());
        return ResponseEntity.ok().body(respones);
    }
}
