package com.causal.order.client.inventory;

import com.causal.order.client.inventory.dto.request.ReservationExtendRequest;
import com.causal.order.client.inventory.dto.request.StockReserveRequest;
import com.causal.order.client.inventory.dto.response.ReservationResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/inventory")
public interface InventoryClient {

    @PostExchange("/stocks/reserve")
    ReservationResponse reserve(@RequestBody StockReserveRequest request);

    @PostExchange("/reservations/extend")
    ReservationResponse extend(@RequestBody ReservationExtendRequest request);
}
