package com.causal.product.client.inventory.dto.request;

public record StockCreateRequest(Long skuId, Long productId, Integer quantity) {}
