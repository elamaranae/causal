package com.causal.product.dto.response;

import java.io.Serializable;

public record ProductListingResponse(
    long id,
    String name,
    String description,
    String primaryThumbnailUrl,
    long categoryId,
    boolean inStock,
    SkuResponseWithoutMedia defaultSku
) implements Serializable {};
