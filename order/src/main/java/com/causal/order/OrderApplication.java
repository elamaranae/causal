package com.causal.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
@EnableScheduling
@ImportHttpServices(group = "cart", basePackages = "com.causal.order.client.cart")
@ImportHttpServices(group = "profile", basePackages = "com.causal.order.client.profile")
@ImportHttpServices(group = "product", basePackages = "com.causal.order.client.product")
@ImportHttpServices(group = "inventory", basePackages = "com.causal.order.client.inventory")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
