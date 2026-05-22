package com.causal.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.causal.cart.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
