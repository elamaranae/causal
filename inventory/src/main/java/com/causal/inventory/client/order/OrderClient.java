package com.causal.inventory.client.order;

import com.causal.inventory.client.order.dto.request.OrderCompleteRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/orders")
public interface OrderClient {

    @PostExchange("/complete/webhook")
    void completeOrder(@RequestBody OrderCompleteRequest request);
}
