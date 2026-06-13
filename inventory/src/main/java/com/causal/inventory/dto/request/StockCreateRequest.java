package com.causal.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockCreateRequest(
    @NotNull Long skuId,
    @NotNull Long productId,
    @NotNull @Min(0) Integer quantity
) {}
