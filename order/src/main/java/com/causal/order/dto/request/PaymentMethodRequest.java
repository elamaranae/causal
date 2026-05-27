package com.causal.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentMethodRequest(
        @NotBlank String type,
        @NotBlank String cardNumber,
        @NotBlank String expiryMonth,
        @NotBlank String expiryYear,
        @NotBlank String cvv,
        @NotBlank String cardholderName
) {}
