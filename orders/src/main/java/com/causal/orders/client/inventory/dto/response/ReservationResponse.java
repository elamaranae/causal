package com.causal.orders.client.inventory.dto.response;

import java.util.List;

public record ReservationResponse(
        long orderId,
        List<ReservationItemResponse> items
) {}
