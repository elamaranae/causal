
package com.causal.cart.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.causal.cart.config.CurrentUser;
import com.causal.cart.dto.request.CartItemCreateRequest;
import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.dto.response.CartShowResponse;
import com.causal.cart.mapper.CartMapper;
import com.causal.cart.model.Cart;
import com.causal.cart.model.CartItem;
import com.causal.cart.repository.CartItemRepository;
import com.causal.cart.repository.CartRepository;

@Service
public class CartItemService {
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final CartMapper mapper;
  private final CurrentUser currentUser;
  private final CartService cartService;

  public CartItemService(CartRepository cartRepository, CartItemRepository cartItemRepository, CartMapper mapper, CurrentUser currentUser, CartService cartService) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.mapper = mapper;
    this.currentUser = currentUser;
    this.cartService = cartService;
  }

  public CartItemShowResponse createCartItem(CartItemCreateRequest request) {
    Cart cart = cartService.getOrCreateCart();
    return mapper.cartItemShowResponseFrom(createItemFromRequest(request, cart));
  }
  
  public CartItem createItemFromRequest(CartItemCreateRequest request, Cart cart) {
    CartItem item = new CartItem();
    item.setCart(cart);
    item.setSkuId(request.skuId());
    item.setQuantity(request.quantity());
    cartItemRepository.save(item);
    return item;
  }
}
