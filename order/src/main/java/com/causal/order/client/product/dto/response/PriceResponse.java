package com.causal.order.client.product.dto.response;

import java.math.BigDecimal;

public record PriceResponse(
        String priceCurrency,
        BigDecimal priceAmount
) {}
