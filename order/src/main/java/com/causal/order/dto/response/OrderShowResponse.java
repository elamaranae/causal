package com.causal.order.dto.response;

import java.util.List;

public record OrderShowResponse(
        long id,
        String status,
        PriceResponse total,
        AddressResponse shippingAddress,
        AddressResponse billingAddress,
        List<OrderItemShowResponse> items
) {}
