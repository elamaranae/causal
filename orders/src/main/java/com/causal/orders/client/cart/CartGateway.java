package com.causal.orders.client.cart;

import com.causal.orders.client.cart.dto.response.CartShowResponse;
import org.springframework.stereotype.Component;

@Component
public class CartGateway {

    private final CartClient client;

    public CartGateway(CartClient client) {
        this.client = client;
    }

    public CartShowResponse getCurrentUserCart() {
        return client.getCurrentUserCart();
    }
}
