package com.causal.inventory.dto.response;

public record ProductStockShowResponse(
        long productId,
        boolean available
) {
}
