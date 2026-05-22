package com.causal.inventory.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockSkuBulkGetRequest(
        @NotNull(message = "skuIds is required")
        @Size(min = 1, max = 100)
        List<@Positive Long> skuIds
) {
}
