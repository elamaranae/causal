package com.causal.cart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.causal.cart.dto.response.CartShowResponse;

import com.causal.cart.service.CartService;

@RestController
public class CartController {
  private final CartService service;

  public CartController(CartService service) {
    this.service = service;
  }

  @GetMapping("cart/me")
  public CartShowResponse getCart() {
    return service.getCurrentUserCart();
  }
}
