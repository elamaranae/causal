package com.causal.inventory.client.order;

import com.causal.inventory.client.order.dto.request.OrderCompleteRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderGateway {

    private final OrderClient client;

    public OrderGateway(OrderClient client) {
        this.client = client;
    }

    public void completeOrder(Long orderId, String status) {
        client.completeOrder(new OrderCompleteRequest(orderId, status));
    }
}
