package com.causal.cart.mapper;

import org.mapstruct.Mapper;

import com.causal.cart.dto.response.CartShowResponse;
import com.causal.cart.model.Cart;

@Mapper(componentModel = "spring")
public interface CartMapper {
  CartShowResponse cartShowResponseFrom(Cart cart);
}
