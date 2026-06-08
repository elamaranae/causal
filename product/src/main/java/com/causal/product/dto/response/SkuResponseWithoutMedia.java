package com.causal.product.dto.response;

import java.io.Serializable;
import java.util.Map;

public record SkuResponseWithoutMedia(
    long id,
    Map<String, Object> attributes,
    Map<String, Object> variantAttributes,
    PriceResponse price
) implements Serializable {};
