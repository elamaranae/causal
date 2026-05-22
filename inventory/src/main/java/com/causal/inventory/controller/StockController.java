package com.causal.inventory.controller;

import com.causal.inventory.dto.request.StockProductBulkGetRequest;
import com.causal.inventory.dto.request.StockSkuBulkGetRequest;
import com.causal.inventory.dto.response.ProductStockShowResponse;
import com.causal.inventory.dto.response.StockShowResponse;
import com.causal.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("inventory/stocks/{sku_id}")
    public StockShowResponse getStock(@PathVariable("sku_id") Long skuId) {
        return stockService.getStock(skuId);
    }

    @PostMapping("inventory/stocks/skus/bulk")
    public List<StockShowResponse> getStocksBySkuIds(@Valid @RequestBody StockSkuBulkGetRequest request) {
        return stockService.getStocksBySkuIds(request.skuIds());
    }

    @PostMapping("inventory/stocks/products/bulk")
    public List<ProductStockShowResponse> getStocksByProductIds(@Valid @RequestBody StockProductBulkGetRequest request) {
        return stockService.getStocksByProductIds(request.productIds());
    }
}
