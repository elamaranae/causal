package com.causal.orders.client.product.dto.response;

public record ProductMinimalResponse(
        long id,
        String name,
        String description,
        long categoryId
) {}
