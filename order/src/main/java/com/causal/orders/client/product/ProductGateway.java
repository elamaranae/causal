package com.causal.orders.client.product;

import com.causal.orders.client.product.dto.request.SkuBulkGetRequest;
import com.causal.orders.client.product.dto.response.SkuShowResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductGateway {

    private final ProductClient client;

    public ProductGateway(ProductClient client) {
        this.client = client;
    }

    public List<SkuShowResponse> getSkusByIds(List<Long> skuIds) {
        return client.getSkusBulk(new SkuBulkGetRequest(skuIds));
    }
}
