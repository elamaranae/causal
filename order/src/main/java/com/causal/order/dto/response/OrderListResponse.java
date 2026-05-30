package com.causal.order.dto.response;

import java.util.List;

public record OrderListResponse(
        List<OrderShowResponse> orders,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
