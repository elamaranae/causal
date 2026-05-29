package com.causal.order.controller;

import com.causal.order.dto.request.OrderCompleteWebhookRequest;
import com.causal.order.dto.request.PaymentRequest;
import com.causal.order.dto.request.PaymentWebhookRequest;
import com.causal.order.dto.response.OrderShowResponse;
import com.causal.order.dto.response.OrderStatusResponse;
import com.causal.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("orders/{id}/pay")
    public void pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        orderService.pay(id, request);
    }

    @PostMapping("orders/payment/webhook")
    public void paymentWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        orderService.handlePaymentWebhook(request.orderId(), request.status());
    }

    @PostMapping("orders/complete/webhook")
    public void completeWebhook(@Valid @RequestBody OrderCompleteWebhookRequest request) {
        orderService.handleOrderCompleteWebhook(request.orderId(), request.status());
    }
}
