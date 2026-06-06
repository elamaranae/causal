package com.causal.cart.controller;

import com.causal.cart.config.SecurityConfig;
import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.dto.response.CartShowResponse;
import com.causal.cart.service.CartItemService;
import com.causal.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CartController.class, CartItemController.class})
@Import(SecurityConfig.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartItemService cartItemService;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("1").claim("email", "test@test.com"));
    }

    @Test
    void getCart_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/cart/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCart_authenticated_returns200() throws Exception {
        CartShowResponse response = new CartShowResponse(1L, 1L,
                List.of(new CartItemShowResponse(1L, 100L, 2)));
        when(cartService.getCurrentUserCart()).thenReturn(response);

        mockMvc.perform(get("/cart/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.items[0].skuId").value(100))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void deleteCart_authenticated_returns200() throws Exception {
        mockMvc.perform(delete("/cart/me").with(jwt()))
                .andExpect(status().isOk());

        verify(cartService).deleteCurrentUserCart();
    }

    @Test
    void deleteCart_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"))
                .when(cartService).deleteCurrentUserCart();

        mockMvc.perform(delete("/cart/me").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCartItem_validRequest_returns200() throws Exception {
        CartItemShowResponse response = new CartItemShowResponse(1L, 100L, 2);
        when(cartItemService.createCartItem(any())).thenReturn(response);

        mockMvc.perform(post("/cart/me/items")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuId": 100, "quantity": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value(100))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void addCartItem_missingSkuId_returns400() throws Exception {
        mockMvc.perform(post("/cart/me/items")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCartItem_invalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/cart/me/items")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuId": 100, "quantity": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCartItem_returns200() throws Exception {
        CartItemShowResponse response = new CartItemShowResponse(1L, 100L, 5);
        when(cartItemService.updateCartItem(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/cart/me/items/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void deleteCartItem_returns200() throws Exception {
        mockMvc.perform(delete("/cart/me/items/1").with(jwt()))
                .andExpect(status().isOk());

        verify(cartItemService).deleteCartItem(1L);
    }
}
