package com.causal.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.cart.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
  @EntityGraph(attributePaths = {"items"})
  public Optional<Cart> findByUserId(long userId);
}
