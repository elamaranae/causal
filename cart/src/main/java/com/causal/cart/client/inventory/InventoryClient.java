package com.causal.cart.client.inventory;

import com.causal.cart.client.inventory.dto.request.StockSkuBulkGetRequest;
import com.causal.cart.client.inventory.dto.response.StockShowResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/internal/inventory")
public interface InventoryClient {

    @GetExchange("/stocks/{skuId}")
    StockShowResponse getStock(@PathVariable Long skuId);

    @PostExchange("/stocks/skus/bulk")
    List<StockShowResponse> getStocksBulk(@RequestBody StockSkuBulkGetRequest request);
}
