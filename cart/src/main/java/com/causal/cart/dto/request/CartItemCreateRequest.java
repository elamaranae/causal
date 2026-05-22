package com.causal.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record CartItemCreateRequest(
    @NotNull Long skuId,
    @NotNull @Min(1) Integer quantity
) {};
