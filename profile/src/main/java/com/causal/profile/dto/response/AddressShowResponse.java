package com.causal.profile.dto.response;

public record AddressShowResponse(
        long id,
        long userId,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String country,
        String pincode,
        String phoneNumber
) {}
