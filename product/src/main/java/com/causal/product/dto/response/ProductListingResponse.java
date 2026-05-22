package com.causal.product.dto.response;

public record ProductListingResponse(
    long id,
    String name,
    String description,
    String primaryThumbnailUrl,
    long categoryId,
    SkuResponseWithoutMedia defaultSku
) {};
