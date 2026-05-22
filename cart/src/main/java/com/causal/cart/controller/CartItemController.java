package com.causal.cart.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.service.CartItemService;
import com.causal.cart.dto.request.CartItemCreateRequest;

@RestController
public class CartItemController {
  private final CartItemService service;

  public CartItemController(CartItemService service) {
    this.service = service;
  }

  @PostMapping("/cart/me/item")
  public CartItemShowResponse createCartItem(@Validated @RequestBody CartItemCreateRequest request) {
    return service.createCartItem(request);
  }
}
