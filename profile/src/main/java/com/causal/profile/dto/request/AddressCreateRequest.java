package com.causal.profile.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressCreateRequest(
        String label,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String country,
        @NotBlank String pincode,
        String phoneNumber
) {}
