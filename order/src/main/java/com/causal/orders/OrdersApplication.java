package com.causal.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
@ImportHttpServices(group = "cart", basePackages = "com.causal.orders.client.cart")
@ImportHttpServices(group = "profile", basePackages = "com.causal.orders.client.profile")
@ImportHttpServices(group = "product", basePackages = "com.causal.orders.client.product")
@ImportHttpServices(group = "inventory", basePackages = "com.causal.orders.client.inventory")
public class OrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
