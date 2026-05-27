package com.causal.orders.dto.response;

public record OrderItemShowResponse(
        long id,
        long skuId,
        int quantity,
        String skuName,
        String skuDescription,
        String deliveryStatus,
        PriceResponse price
) {}
