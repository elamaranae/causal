package com.causal.product.dto.response;

import java.util.List;
import java.util.Map;

public record ProductShowResponse(
    long id,
    String name,
    String description,
    String primaryThumbnailUrl,
    long categoryId,
    String primaryVariantKey,
    Map<String, Object> attributes,
    long defaultSkuId,
    List<SkuResponse> skus
) {};
