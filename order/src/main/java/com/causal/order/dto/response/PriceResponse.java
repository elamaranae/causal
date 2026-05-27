package com.causal.order.dto.response;

import java.math.BigDecimal;

public record PriceResponse(
        String priceCurrency,
        BigDecimal priceAmount
) {}
