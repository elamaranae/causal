package com.causal.orders.dto.response;

import java.util.List;

public record OrderShowResponse(
        long id,
        String status,
        PriceResponse total,
        List<OrderItemShowResponse> items
) {}
