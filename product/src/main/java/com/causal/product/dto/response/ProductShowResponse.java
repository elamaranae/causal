package com.causal.product.dto.response;

import java.util.List;
import java.util.Map;

public record ProductShowResponse(
    long id,
    String name,
    String primaryThumbnailUrl,
    long categoryId,
    String primaryVariantKey,
    Map<String, Object> attributes,
    List<SkuResponse> skus
) {};
