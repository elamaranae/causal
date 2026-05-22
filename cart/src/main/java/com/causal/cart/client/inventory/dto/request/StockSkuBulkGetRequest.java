package com.causal.cart.client.inventory.dto.request;

import java.util.List;

public record StockSkuBulkGetRequest(
        List<Long> skuIds
) {
}
