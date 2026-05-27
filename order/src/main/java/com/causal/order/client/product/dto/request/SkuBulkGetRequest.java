package com.causal.order.client.product.dto.request;

import java.util.List;

public record SkuBulkGetRequest(
        List<Long> ids
) {}
