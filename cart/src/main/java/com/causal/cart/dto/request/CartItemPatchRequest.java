package com.causal.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record CartItemPatchRequest(
    @NotNull @Min(1) Integer quantity
) {};
