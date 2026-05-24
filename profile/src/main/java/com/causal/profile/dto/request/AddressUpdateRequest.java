package com.causal.profile.dto.request;

public record AddressUpdateRequest(
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String country,
        String pincode,
        String phoneNumber
) {}
