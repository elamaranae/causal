package com.causal.orders.client.inventory;

import com.causal.orders.client.inventory.dto.request.StockReserveRequest;
import com.causal.orders.client.inventory.dto.response.ReservationResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/inventory")
public interface InventoryClient {

    @PostExchange("/stocks/reserve")
    ReservationResponse reserve(@RequestBody StockReserveRequest request);
}
