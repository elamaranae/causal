package com.causal.orders.dto.response;

import java.math.BigDecimal;

public record OrderItemShowResponse(long id, long skuId, int quantity, BigDecimal price) {
}
