package com.causal.orders.dto.response;

public record AddressResponse(
        long id,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String country,
        String pincode,
        String phoneNumber
) {}
