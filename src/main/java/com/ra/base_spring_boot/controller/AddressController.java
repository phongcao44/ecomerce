package com.ra.base_spring_boot.controller;


import com.ra.base_spring_boot.dto.req.AddressRequest;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.IAddressService;
import com.ra.base_spring_boot.services.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/address")
public class AddressController {

    private final IAddressService addressService;
    private final IUserService userService;
    public AddressController(IAddressService addressService, IUserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAddress(@Valid @RequestBody AddressRequest addressRequest
    , @AuthenticationPrincipal MyUserDetails user)
    {
        long Id = user.getUser().getId();
        addressService.addAddress(Id, addressRequest);
        return ResponseEntity.ok().body(addressRequest);
    }
    @PutMapping("/{Id}/update")
    public ResponseEntity<?> updateAddress(@Valid @RequestBody AddressRequest addressRequest
            , @AuthenticationPrincipal MyUserDetails user, @PathVariable long Id)
    {
        long userId = user.getUser().getId();
        addressService.update(userId,Id, addressRequest);
        return ResponseEntity.ok().body(addressRequest);
    }
    @DeleteMapping("/{Id}/delete")
    public  ResponseEntity<?> deleteAddress(@PathVariable long Id, @AuthenticationPrincipal MyUserDetails user)
    {
        long userId = user.getUser().getId();
        addressService.delete(userId,Id);
        return ResponseEntity.ok().body("<UNK>");

    }
}
