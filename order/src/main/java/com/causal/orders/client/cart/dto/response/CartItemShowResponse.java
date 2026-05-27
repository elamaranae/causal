package com.causal.orders.client.cart.dto.response;

public record CartItemShowResponse(
        long id,
        long skuId,
        int quantity
) {}
