package com.causal.profile.dto.request;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(min = 3, max = 3) String currency,
        Long defaultAddressId
) {}
