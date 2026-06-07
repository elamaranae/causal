package com.causal.order.dto.request;

import com.causal.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentWebhookRequest(
        @NotNull Long orderId,
        @NotNull OrderStatus status
) {}
