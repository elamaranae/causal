package com.causal.orders.repository;

import com.causal.orders.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "shippingAddress", "billingAddress"})
    Optional<Order> findDetailById(Long id);
}
