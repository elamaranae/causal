package com.causal.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateProductRequest(
    @NotBlank String name,
    String description,
    @NotBlank String categoryName,
    String primaryThumbnailUrl,
    String primaryVariantKey,
    @NotEmpty @Valid List<CreateSkuRequest> skus
) {
    public record CreateSkuRequest(
        Map<String, Object> variantAttributes,
        @NotNull @Positive BigDecimal price,
        @NotBlank String currency,
        @NotNull @Min(0) Integer stock
    ) {}
}
