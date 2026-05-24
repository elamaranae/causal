package com.causal.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Size(min = 3, max = 3) String currency
) {}
