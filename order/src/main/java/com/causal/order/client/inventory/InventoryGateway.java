package com.causal.order.client.inventory;

import com.causal.order.client.inventory.dto.request.StockReservationItemRequest;
import com.causal.order.client.inventory.dto.request.StockReserveRequest;
import com.causal.order.client.inventory.dto.response.ReservationResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryGateway {

    private final InventoryClient client;

    public InventoryGateway(InventoryClient client) {
        this.client = client;
    }

    public ReservationResponse reserve(Long orderId, List<StockReservationItemRequest> items) {
        return client.reserve(new StockReserveRequest(orderId, items));
    }
}
