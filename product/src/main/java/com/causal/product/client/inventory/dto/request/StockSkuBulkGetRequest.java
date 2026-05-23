package com.causal.product.client.inventory.dto.request;

import java.util.List;

public record StockSkuBulkGetRequest(
        List<Long> skuIds
) {
}
