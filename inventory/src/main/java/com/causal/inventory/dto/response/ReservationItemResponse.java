package com.causal.inventory.dto.response;

import java.time.Instant;

public record ReservationItemResponse(
        long skuId,
        int quantity,
        Instant expiresAt
) {}
