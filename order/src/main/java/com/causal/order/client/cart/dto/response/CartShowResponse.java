package com.causal.order.client.cart.dto.response;

import java.util.List;

public record CartShowResponse(
        long id,
        long userId,
        List<CartItemShowResponse> items
) {}
