package com.causal.order.client.inventory.dto.request;

import java.util.List;

public record ReservationExtendRequest(
        Long userId,
        Long orderId,
        List<StockReservationItemRequest> items
) {}
