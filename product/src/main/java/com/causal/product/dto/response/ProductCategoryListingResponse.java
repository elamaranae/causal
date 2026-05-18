package com.causal.product.dto.response;

public record ProductCategoryListingResponse(
    long id,
    String name,
    String description,
    Long parentId
) {}
