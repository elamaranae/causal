package com.causal.product.dto.response;

import java.io.Serializable;

public record ProductCategoryListingResponse(
    long id,
    String name,
    String description,
    Long parentId
) implements Serializable {}
