package com.causal.order.dto.response;

import com.causal.order.model.OrderStatus;

import java.util.List;

public record OrderShowResponse(
        long id,
        OrderStatus status,
        PriceResponse total,
        AddressResponse shippingAddress,
        AddressResponse billingAddress,
        List<OrderItemShowResponse> items
) {}
