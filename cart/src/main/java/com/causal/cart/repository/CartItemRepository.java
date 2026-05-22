package com.causal.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.cart.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  public Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
}
