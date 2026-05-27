package com.causal.orders.client.cart;

import com.causal.orders.client.cart.dto.response.CartShowResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/cart")
public interface CartClient {

    @GetExchange("/me")
    CartShowResponse getCurrentUserCart();
}
