package com.causal.order.controller;

import com.causal.order.dto.response.OrderShowResponse;
import com.causal.order.dto.response.OrderStatusResponse;
import com.causal.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("orders/{id}")
    public OrderShowResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping("orders/{id}/status")
    public OrderStatusResponse getOrderStatus(@PathVariable Long id) {
        return orderService.getOrderStatus(id);
    }

    @PostMapping("orders/checkout")
    public OrderShowResponse checkout() {
        return orderService.checkout();
    }
}
