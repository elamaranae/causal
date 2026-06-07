package com.causal.product.client.inventory;

import com.causal.product.client.inventory.dto.request.StockProductBulkGetRequest;
import com.causal.product.client.inventory.dto.request.StockSkuBulkGetRequest;
import com.causal.product.client.inventory.dto.response.ProductStockShowResponse;
import com.causal.product.client.inventory.dto.response.StockShowResponse;
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

    @PostExchange("/stocks/products/bulk")
    List<ProductStockShowResponse> getStocksByProductIds(@RequestBody StockProductBulkGetRequest request);
}
