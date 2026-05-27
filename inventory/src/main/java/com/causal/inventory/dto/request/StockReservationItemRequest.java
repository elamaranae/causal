package com.causal.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockReservationItemRequest(
        @NotNull @Positive Long skuId,
        @NotNull @Positive Integer quantity
) {}
