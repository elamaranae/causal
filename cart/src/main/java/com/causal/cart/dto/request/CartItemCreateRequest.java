package com.causal.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemCreateRequest(
    @NotNull @Positive Long skuId,
    @NotNull @Positive Integer quantity
) {}
