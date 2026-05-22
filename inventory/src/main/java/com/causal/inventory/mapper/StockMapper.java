package com.causal.inventory.mapper;

import com.causal.inventory.dto.response.StockShowResponse;
import com.causal.inventory.model.Stock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockMapper {

    StockShowResponse from(Stock stock);
}
