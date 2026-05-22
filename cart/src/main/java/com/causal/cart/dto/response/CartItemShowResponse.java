package com.causal.cart.dto.response;

public record CartItemShowResponse(
    Long id,
    Long skuId,
    Integer quantity
) {};
