package com.causal.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull @Valid PaymentMethodRequest paymentMethod
) {}
