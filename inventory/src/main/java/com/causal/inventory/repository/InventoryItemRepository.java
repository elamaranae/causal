package com.causal.inventory.repository;

import com.causal.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySkuIdAndWarehouseId(Long skuId, Long warehouseId);
}
