package com.causal.cart.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.service.CartItemService;
import com.causal.cart.dto.request.CartItemCreateRequest;
import com.causal.cart.dto.request.CartItemPatchRequest;

@RestController
public class CartItemController {
  private final CartItemService service;

  public CartItemController(CartItemService service) {
    this.service = service;
  }

  @PostMapping("/cart/me/items")
  public CartItemShowResponse createCartItem(@Validated @RequestBody CartItemCreateRequest request) {
    return service.createCartItem(request);
  }

  @PatchMapping("/cart/me/items/{id}")
  public CartItemShowResponse createCartItem(@PathVariable("id") Long id, @Validated @RequestBody CartItemPatchRequest request) {
    return service.updateCartItem(id, request);
  }

  @DeleteMapping("/cart/me/items/{id}")
  public void createCartItem(@PathVariable("id") Long id) {
    service.deleteCartItem(id);
  }
}
