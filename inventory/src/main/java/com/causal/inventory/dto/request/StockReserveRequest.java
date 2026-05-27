package com.causal.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record StockReserveRequest(
        @NotNull @Positive Long orderId,
        @NotEmpty List<@Valid StockReservationItemRequest> items
) {}
