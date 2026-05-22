package com.causal.cart.client.inventory;

import com.causal.cart.client.inventory.dto.request.StockSkuBulkGetRequest;
import com.causal.cart.client.inventory.dto.response.StockShowResponse;
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
}
