package com.causal.cart.mapper;

import org.mapstruct.Mapper;

import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.dto.response.CartShowResponse;
import com.causal.cart.model.Cart;
import com.causal.cart.model.CartItem;

@Mapper(componentModel = "spring")
public interface CartMapper {
  CartShowResponse cartShowResponseFrom(Cart cart);
  CartItemShowResponse cartItemShowResponseFrom(CartItem cartItem);
}
