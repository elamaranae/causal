package com.causal.order.client.inventory.dto.request;

public record StockReservationItemRequest(
        Long skuId,
        Integer quantity
) {}
