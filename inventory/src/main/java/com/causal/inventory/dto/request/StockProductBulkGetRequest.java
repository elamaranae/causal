package com.causal.inventory.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockProductBulkGetRequest(
        @NotNull(message = "productIds is required")
        @Size(min = 1, max = 100)
        List<@Positive Long> productIds
) {
}
