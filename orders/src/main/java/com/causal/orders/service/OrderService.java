package com.causal.orders.service;

import com.causal.orders.dto.response.OrderShowResponse;
import com.causal.orders.mapper.OrderMapper;
import com.causal.orders.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    public OrderShowResponse getOrder(Long id) {
        return orderRepository.findDetailById(id)
                .map(orderMapper::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
