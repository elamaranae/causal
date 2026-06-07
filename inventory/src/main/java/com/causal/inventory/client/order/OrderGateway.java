package com.causal.inventory.client.order;

import com.causal.inventory.client.order.dto.request.OrderCompleteRequest;
import com.causal.inventory.model.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderGateway {

    private final OrderClient client;

    public OrderGateway(OrderClient client) {
        this.client = client;
    }

    public void completeOrder(Long orderId, OrderStatus status) {
        client.completeOrder(new OrderCompleteRequest(orderId, status));
    }
}
