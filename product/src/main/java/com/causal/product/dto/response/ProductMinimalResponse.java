package com.causal.product.dto.response;

public record ProductMinimalResponse(
    long id,
    String name,
    String description,
    long categoryId
) {}
