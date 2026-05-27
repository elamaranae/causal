package com.causal.orders.client.inventory.dto.request;

public record StockReservationItemRequest(
        Long skuId,
        Integer quantity
) {}
