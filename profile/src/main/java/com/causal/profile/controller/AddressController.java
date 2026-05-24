package com.causal.profile.controller;

import com.causal.profile.dto.request.AddressCreateRequest;
import com.causal.profile.dto.request.AddressUpdateRequest;
import com.causal.profile.dto.response.AddressShowResponse;
import com.causal.profile.service.AddressService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/profiles/me/addresses")
    public List<AddressShowResponse> getAddresses() {
        return addressService.getAddresses();
    }

    @PostMapping("/profiles/me/addresses")
    public AddressShowResponse createAddress(@Validated @RequestBody AddressCreateRequest request) {
        return addressService.createAddress(request);
    }

    @PatchMapping("/profiles/me/addresses/{id}")
    public AddressShowResponse updateAddress(@PathVariable Long id, @Validated @RequestBody AddressUpdateRequest request) {
        return addressService.updateAddress(id, request);
    }

    @DeleteMapping("/profiles/me/addresses/{id}")
    public void deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
    }
}
