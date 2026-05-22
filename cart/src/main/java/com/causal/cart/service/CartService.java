package com.causal.cart.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.causal.cart.config.CurrentUser;
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
  private final CurrentUser currentUser;

  public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, CartMapper mapper, CurrentUser currentUser) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.mapper = mapper;
    this.currentUser = currentUser;
  }

  public CartShowResponse getCurrentUserCart() {
    Cart cart = getOrCreateCart();
    return mapper.cartShowResponseFrom(cart);
  }

  @Transactional
  public void deleteCurrentUserCart() {
    Cart cart = cartRepository.findByUserId(currentUser.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
    cartRepository.delete(cart);
  }

  @Transactional
  public Cart getOrCreateCart() {
    Cart cart = cartRepository.findByUserId(currentUser.id()).orElseGet(() -> createCart());
    return cart;
  }

  @Transactional
  public Cart createCart() {
    Cart cart = new Cart();
    cart.setUserId(currentUser.id());
    cartRepository.save(cart);
    return cart;
  }
}
