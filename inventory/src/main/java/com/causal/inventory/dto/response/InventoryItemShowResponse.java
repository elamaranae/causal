package com.causal.inventory.dto.response;

public record InventoryItemShowResponse(
        long id,
        long skuId,
        long warehouseId,
        int quantity
) {
}
