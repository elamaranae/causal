package com.causal.cart.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.causal.cart.dto.response.CartShowResponse;
import com.causal.cart.mapper.CartMapper;
import com.causal.cart.model.Cart;
import com.causal.cart.repository.CartItemRepository;
import com.causal.cart.repository.CartRepository;

@Service
public class CartService {
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final CartMapper mapper;

  public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, CartMapper mapper) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.mapper = mapper;
  }

  public CartShowResponse getCurrentUserCart() {
    Cart cart = cartRepository.findFirstBy().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
    return mapper.cartShowResponseFrom(cart);
  }
}
