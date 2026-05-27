package com.causal.orders.client.inventory.dto.request;

import java.util.List;

public record StockReserveRequest(
        Long orderId,
        List<StockReservationItemRequest> items
) {}
