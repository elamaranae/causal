package com.causal.product.client.inventory;

import com.causal.product.client.inventory.dto.request.StockCreateRequest;
import com.causal.product.client.inventory.dto.request.StockProductBulkGetRequest;
import com.causal.product.client.inventory.dto.request.StockSkuBulkGetRequest;
import com.causal.product.client.inventory.dto.response.ProductStockShowResponse;
import com.causal.product.client.inventory.dto.response.StockShowResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryGateway {

    private final InventoryClient client;

    public InventoryGateway(InventoryClient client) {
        this.client = client;
    }

    public StockShowResponse getStock(Long skuId) {
        return client.getStock(skuId);
    }

    public List<StockShowResponse> getStocksBySkuIds(List<Long> skuIds) {
        return client.getStocksBulk(new StockSkuBulkGetRequest(skuIds));
    }

    public List<ProductStockShowResponse> getStocksByProductIds(List<Long> productIds) {
        return client.getStocksByProductIds(new StockProductBulkGetRequest(productIds));
    }

    public StockShowResponse createStock(Long skuId, Long productId, int quantity) {
        return client.createStock(new StockCreateRequest(skuId, productId, quantity));
    }
}
