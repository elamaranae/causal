package com.causal.inventory.client.order.dto.request;

import com.causal.inventory.model.OrderStatus;

public record OrderCompleteRequest(Long orderId, OrderStatus status) {}
