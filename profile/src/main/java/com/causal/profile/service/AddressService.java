package com.causal.profile.service;

import com.causal.profile.config.CurrentUser;
import com.causal.profile.dto.request.AddressCreateRequest;
import com.causal.profile.dto.request.AddressUpdateRequest;
import com.causal.profile.dto.response.AddressShowResponse;
import com.causal.profile.mapper.ProfileMapper;
import com.causal.profile.model.Address;
import com.causal.profile.repository.AddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final ProfileMapper profileMapper;
    private final CurrentUser currentUser;

    public AddressService(AddressRepository addressRepository, ProfileMapper profileMapper, CurrentUser currentUser) {
        this.addressRepository = addressRepository;
        this.profileMapper = profileMapper;
        this.currentUser = currentUser;
    }

    public List<AddressShowResponse> getAddresses() {
        return addressRepository.findByUserId(currentUser.id())
                .stream()
                .map(profileMapper::from)
                .toList();
    }

    public AddressShowResponse createAddress(AddressCreateRequest request) {
        Address address = new Address();
        address.setUserId(currentUser.id());
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setCountry(request.country());
        address.setPincode(request.pincode());
        address.setPhoneNumber(request.phoneNumber());
        addressRepository.save(address);
        return profileMapper.from(address);
    }

    @Transactional
    public AddressShowResponse updateAddress(Long id, AddressUpdateRequest request) {
        Address address = getAddress(id);
        if (request.label() != null) {
            address.setLabel(request.label());
        }
        if (request.line1() != null) {
            address.setLine1(request.line1());
        }
        if (request.line2() != null) {
            address.setLine2(request.line2());
        }
        if (request.city() != null) {
            address.setCity(request.city());
        }
        if (request.state() != null) {
            address.setState(request.state());
        }
        if (request.country() != null) {
            address.setCountry(request.country());
        }
        if (request.pincode() != null) {
            address.setPincode(request.pincode());
        }
        if (request.phoneNumber() != null) {
            address.setPhoneNumber(request.phoneNumber());
        }
        return profileMapper.from(address);
    }

    public void deleteAddress(Long id) {
        Address address = getAddress(id);
        addressRepository.delete(address);
    }

    private Address getAddress(Long id) {
        return addressRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }
}
