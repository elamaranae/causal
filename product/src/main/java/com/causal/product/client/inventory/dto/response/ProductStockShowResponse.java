package com.causal.product.client.inventory.dto.response;

public record ProductStockShowResponse(
        long productId,
        boolean available
) {
}
