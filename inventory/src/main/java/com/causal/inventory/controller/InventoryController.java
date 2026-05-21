package com.causal.inventory.controller;

import com.causal.inventory.dto.response.InventoryItemShowResponse;
import com.causal.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("inventory/stock")
    public InventoryItemShowResponse getStock(@RequestParam Long skuId,
                                              @RequestParam Long warehouseId) {
        return inventoryService.getStock(skuId, warehouseId);
    }
}
