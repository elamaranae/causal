package com.causal.product.dto.response;

import java.util.Map;

public record SkuShowResponse(
    long id,
    Map<String, Object> attributes,
    Map<String, Object> variantAttributes,
    MediaResponse media,
    PriceResponse price,
    ProductMinimalResponse product
) {}
