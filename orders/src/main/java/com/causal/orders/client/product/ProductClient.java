package com.causal.orders.client.product;

import com.causal.orders.client.product.dto.request.SkuBulkGetRequest;
import com.causal.orders.client.product.dto.response.SkuShowResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange
public interface ProductClient {

    @PostExchange("/products/skus/bulk")
    List<SkuShowResponse> getSkusBulk(@RequestBody SkuBulkGetRequest request);
}
