package com.causal.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.cart.model.Cart;

public interface CartItemRepository extends JpaRepository<Cart, Long> {
  public Optional<Cart> findFirstBy();
}
