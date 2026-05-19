package com.causal.product.dto.response;

import java.util.Map;
import java.util.List;

public record MediaResponse(
    long id,
    List<Map<String, Object>> media
) {};
