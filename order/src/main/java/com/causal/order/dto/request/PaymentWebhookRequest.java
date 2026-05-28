package com.causal.order.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentWebhookRequest(
        @NotNull Long orderId,
        @NotNull String status
) {}
