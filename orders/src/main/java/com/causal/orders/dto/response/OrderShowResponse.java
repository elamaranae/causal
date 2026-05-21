package com.causal.orders.dto.response;

import java.math.BigDecimal;

public record OrderShowResponse(long id, long userId, String status, BigDecimal totalAmount) {
}
