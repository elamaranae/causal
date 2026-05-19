package com.causal.product.dto.response;

import java.util.Map;

public record SkuResponse(
    long id,
    boolean isDefault,
    Map<String, Object> attributes,
    Map<String, Object> variantAttributes,
    MediaResponse media
) {};
