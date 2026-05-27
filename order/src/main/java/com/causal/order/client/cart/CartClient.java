package com.causal.order.client.cart;

import com.causal.order.client.cart.dto.response.CartShowResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/cart")
public interface CartClient {

    @GetExchange("/me")
    CartShowResponse getCurrentUserCart();
}
