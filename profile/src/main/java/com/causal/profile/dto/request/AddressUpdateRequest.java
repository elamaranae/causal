package com.causal.profile.dto.request;

import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @Size(max = 100) String label,
        @Size(max = 255) String line1,
        @Size(max = 255) String line2,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 100) String country,
        @Size(max = 20) String pincode,
        @Size(max = 50) String phoneNumber
) {}
