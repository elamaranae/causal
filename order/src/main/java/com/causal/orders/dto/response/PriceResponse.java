package com.causal.orders.dto.response;

import java.math.BigDecimal;

public record PriceResponse(
        String priceCurrency,
        BigDecimal priceAmount
) {}
