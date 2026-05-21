package com.causal.inventory.mapper;

import com.causal.inventory.dto.response.InventoryItemShowResponse;
import com.causal.inventory.model.InventoryItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryItemMapper {

    InventoryItemShowResponse from(InventoryItem item);
}
