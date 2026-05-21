package com.causal.inventory.service;

import com.causal.inventory.dto.response.InventoryItemShowResponse;
import com.causal.inventory.mapper.InventoryItemMapper;
import com.causal.inventory.model.InventoryItem;
import com.causal.inventory.repository.InventoryItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryItemMapper inventoryItemMapper;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryItemMapper inventoryItemMapper) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryItemMapper = inventoryItemMapper;
    }

    public InventoryItemShowResponse getStock(Long skuId, Long warehouseId) {
        InventoryItem item = inventoryItemRepository
                .findBySkuIdAndWarehouseId(skuId, warehouseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Stock not found for skuId=" + skuId + " warehouseId=" + warehouseId));
        return inventoryItemMapper.from(item);
    }
}
