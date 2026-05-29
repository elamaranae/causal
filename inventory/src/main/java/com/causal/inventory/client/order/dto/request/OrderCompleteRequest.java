package com.causal.inventory.client.order.dto.request;

public record OrderCompleteRequest(Long orderId, String status) {}
