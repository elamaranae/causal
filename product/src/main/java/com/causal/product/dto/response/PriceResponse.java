package com.causal.product.dto.response;

import java.math.BigDecimal;

public record PriceResponse(
    String priceCurrency,
    BigDecimal priceAmount
) {}
