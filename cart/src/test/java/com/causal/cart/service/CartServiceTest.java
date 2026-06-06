package com.causal.cart.service;

import com.causal.cart.config.CurrentUser;
import com.causal.cart.mapper.CartMapper;
import com.causal.cart.model.Cart;
import com.causal.cart.repository.CartItemRepository;
import com.causal.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private CartMapper mapper;
    @Mock private CurrentUser currentUser;

    @InjectMocks
    private CartService cartService;

    @Test
    void deleteCurrentUserCart_noCart_throws() {
        when(currentUser.id()).thenReturn(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> cartService.deleteCurrentUserCart());
    }

    @Test
    void deleteCurrentUserCart_happyPath() {
        when(currentUser.id()).thenReturn(1L);
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.deleteCurrentUserCart();

        verify(cartRepository).delete(cart);
    }

    @Test
    void getOrCreateCart_existingCart_returnsIt() {
        when(currentUser.id()).thenReturn(1L);
        Cart existing = new Cart();
        existing.setId(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        Cart result = cartService.getOrCreateCart();
        assertSame(existing, result);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_noCart_createsNew() {
        when(currentUser.id()).thenReturn(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.getOrCreateCart();
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(cartRepository).save(any(Cart.class));
    }
}
