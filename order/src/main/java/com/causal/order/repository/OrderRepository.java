package com.causal.order.repository;

import com.causal.order.dto.response.OrderStatusResponse;
import com.causal.order.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "shippingAddress", "billingAddress"})
    Optional<Order> findDetailById(Long id);

    Optional<OrderStatusResponse> findStatusById(Long id);
}
