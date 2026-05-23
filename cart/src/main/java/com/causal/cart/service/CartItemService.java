
package com.causal.cart.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.causal.cart.client.inventory.InventoryGateway;
import com.causal.cart.client.inventory.dto.response.StockShowResponse;
import com.causal.cart.config.CurrentUser;
import com.causal.cart.dto.request.CartItemCreateRequest;
import com.causal.cart.dto.request.CartItemPatchRequest;
import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.dto.response.CartShowResponse;
import com.causal.cart.mapper.CartMapper;
import com.causal.cart.model.Cart;
import com.causal.cart.model.CartItem;
import com.causal.cart.repository.CartItemRepository;
import com.causal.cart.repository.CartRepository;

import java.util.List;

@Service
public class CartItemService {
  private static final int MAX_CART_ITEMS = 100;

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final CartMapper mapper;
  private final CurrentUser currentUser;
  private final CartService cartService;
  private final InventoryGateway inventoryGateway;

  public CartItemService(CartRepository cartRepository, CartItemRepository cartItemRepository, CartMapper mapper, CurrentUser currentUser, CartService cartService, InventoryGateway inventoryGateway) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.mapper = mapper;
    this.currentUser = currentUser;
    this.cartService = cartService;
    this.inventoryGateway = inventoryGateway;
  }

  public CartItemShowResponse createCartItem(CartItemCreateRequest request) {
    Cart cart = cartService.getOrCreateCart();
    long itemCount = cartItemRepository.countByCartId(cart.getId());
    if (itemCount >= MAX_CART_ITEMS) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cart cannot exceed " + MAX_CART_ITEMS + " items");
    }
    validateStockAvailability(request.skuId(), request.quantity());
    return mapper.cartItemShowResponseFrom(createItemFromRequest(request, cart));
  }

  @Transactional
  public CartItemShowResponse updateCartItem(Long id, CartItemPatchRequest request) {
    CartItem cartItem = getCartItem(id);
    validateStockAvailability(cartItem.getSkuId(), request.quantity());
    cartItem.setQuantity(request.quantity());
    return mapper.cartItemShowResponseFrom(cartItem);
  }

  public void deleteCartItem(Long id) {
    CartItem cartItem = getCartItem(id);
    cartItemRepository.delete(cartItem);
  }

  public CartItem getCartItem(Long id) {
    Cart cart = cartService.getOrCreateCart();
    CartItem cartItem = cartItemRepository.findByIdAndCartId(id, cart.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart Item not found"));
    return cartItem;
  }
  
  private void validateStockAvailability(Long skuId, int quantity) {
    List<StockShowResponse> stocks = inventoryGateway.getStocksBySkuIds(List.of(skuId));
    if (stocks.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SKU not found in inventory");
    }
    StockShowResponse stock = stocks.getFirst();
    if (stock.quantity() < quantity) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          "Insufficient stock. Available: " + stock.quantity() + ", requested: " + quantity);
    }
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
