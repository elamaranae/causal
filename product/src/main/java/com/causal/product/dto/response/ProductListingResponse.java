package com.causal.product.dto.response;

public record ProductListingResponse(
    long id,
    String name,
    String primaryThumbnailUrl,
    long categoryId
) {};
