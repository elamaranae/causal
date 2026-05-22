package com.causal.inventory.dto.response;

public record StockShowResponse(
        long id,
        long skuId,
        long productId,
        int quantity
) {
}
