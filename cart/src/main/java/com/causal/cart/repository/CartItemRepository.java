package com.causal.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.causal.cart.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  public Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
  public long countByCartId(Long cartId);

  @Query(value = "SELECT add_cart_item_with_limit(:cartId, :skuId, :quantity, :maxItems)", nativeQuery = true)
  long addWithLimit(Long cartId, Long skuId, Integer quantity, Integer maxItems);
}
