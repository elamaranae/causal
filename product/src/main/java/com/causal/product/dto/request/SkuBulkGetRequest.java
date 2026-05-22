package com.causal.product.dto.request;

import java.util.List;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record SkuBulkGetRequest(
    @NotNull(message = "ids is required")
    @Size(min = 1, max = 100)
    List<@Positive Long> ids
) {}
