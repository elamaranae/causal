package com.causal.cart.dto.response;

import java.util.List;

public record CartShowResponse(
    Long id,
    Long userId,
    List<CartItemShowResponse> items
) {};
