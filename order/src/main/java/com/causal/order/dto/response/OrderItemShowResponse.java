package com.causal.order.dto.response;

import com.causal.order.model.DeliveryStatus;

public record OrderItemShowResponse(
        long id,
        long skuId,
        int quantity,
        String skuName,
        String skuDescription,
        DeliveryStatus deliveryStatus,
        PriceResponse price
) {}
