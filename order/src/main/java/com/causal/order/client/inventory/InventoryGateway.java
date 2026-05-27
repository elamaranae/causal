package com.causal.order.client.inventory;

import com.causal.order.client.inventory.dto.request.ReservationExtendRequest;
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

    public ReservationResponse reserve(Long userId, Long orderId, List<StockReservationItemRequest> items) {
        return client.reserve(new StockReserveRequest(userId, orderId, items));
    }

    public ReservationResponse extendReservation(Long userId, Long orderId, List<StockReservationItemRequest> items) {
        return client.extend(new ReservationExtendRequest(userId, orderId, items));
    }
}
