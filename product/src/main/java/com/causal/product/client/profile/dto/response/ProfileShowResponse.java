package com.causal.product.client.profile.dto.response;

public record ProfileShowResponse(
        long id,
        long userId,
        String firstName,
        String lastName,
        String currency,
        Long defaultAddressId
) {}
