package com.causal.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(
        @NotBlank @Size(max = 50) String type,
        @NotBlank @Size(max = 19) String cardNumber,
        @NotBlank @Size(max = 2) String expiryMonth,
        @NotBlank @Size(max = 4) String expiryYear,
        @NotBlank @Size(max = 4) String cvv,
        @NotBlank @Size(max = 100) String cardholderName
) {}
