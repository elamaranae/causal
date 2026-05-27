package com.causal.order.client.product.dto.response;

import java.util.Map;

public record SkuShowResponse(
        long id,
        Map<String, Object> attributes,
        Map<String, Object> variantAttributes,
        PriceResponse price,
        int stockQuantity,
        ProductMinimalResponse product
) {}
