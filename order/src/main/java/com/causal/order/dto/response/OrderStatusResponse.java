package com.causal.order.dto.response;

import com.causal.order.model.OrderStatus;

public record OrderStatusResponse(
        long id,
        OrderStatus status
) {}
