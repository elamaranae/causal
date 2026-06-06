package com.causal.cart.service;

import com.causal.cart.client.inventory.InventoryGateway;
import com.causal.cart.client.inventory.dto.response.StockShowResponse;
import com.causal.cart.config.CurrentUser;
import com.causal.cart.dto.request.CartItemCreateRequest;
import com.causal.cart.dto.request.CartItemPatchRequest;
import com.causal.cart.mapper.CartMapper;
import com.causal.cart.model.Cart;
import com.causal.cart.model.CartItem;
import com.causal.cart.dto.response.CartItemShowResponse;
import com.causal.cart.repository.CartItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private CartMapper mapper;
    @Mock private CurrentUser currentUser;
    @Mock private CartService cartService;
    @Mock private InventoryGateway inventoryGateway;

    @InjectMocks
    private CartItemService cartItemService;

    @Test
    void createCartItem_insufficientStock_throws() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(inventoryGateway.getStocksBySkuIds(List.of(100L)))
                .thenReturn(List.of(new StockShowResponse(1L, 100L, 1L, 3)));

        CartItemCreateRequest request = new CartItemCreateRequest(100L, 5);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartItemService.createCartItem(request));
        assertTrue(ex.getReason().contains("Insufficient stock"));
    }

    @Test
    void createCartItem_skuNotInInventory_throws() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(inventoryGateway.getStocksBySkuIds(anyList())).thenReturn(List.of());

        CartItemCreateRequest request = new CartItemCreateRequest(999L, 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartItemService.createCartItem(request));
        assertTrue(ex.getReason().contains("SKU not found"));
    }

    @Test
    void createCartItem_exceedsMaxItems_throws() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(inventoryGateway.getStocksBySkuIds(List.of(100L)))
                .thenReturn(List.of(new StockShowResponse(1L, 100L, 1L, 10)));
        when(cartItemRepository.addWithLimit(1L, 100L, 2, 100)).thenReturn(-1L);

        CartItemCreateRequest request = new CartItemCreateRequest(100L, 2);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartItemService.createCartItem(request));
        assertTrue(ex.getReason().contains("cannot exceed"));
    }

    @Test
    void createCartItem_happyPath() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(inventoryGateway.getStocksBySkuIds(List.of(100L)))
                .thenReturn(List.of(new StockShowResponse(1L, 100L, 1L, 10)));
        when(cartItemRepository.addWithLimit(1L, 100L, 2, 100)).thenReturn(5L);

        CartItem item = new CartItem();
        item.setSkuId(100L);
        item.setQuantity(2);
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(mapper.cartItemShowResponseFrom(item))
                .thenReturn(new CartItemShowResponse(5L, 100L, 2));

        CartItemShowResponse response = cartItemService.createCartItem(new CartItemCreateRequest(100L, 2));
        assertEquals(100L, response.skuId());
        assertEquals(2, response.quantity());
    }

    @Test
    void updateCartItem_insufficientStock_throws() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);

        CartItem existing = new CartItem();
        existing.setSkuId(100L);
        existing.setQuantity(1);
        when(cartItemRepository.findByIdAndCartId(1L, 1L)).thenReturn(Optional.of(existing));
        when(inventoryGateway.getStocksBySkuIds(List.of(100L)))
                .thenReturn(List.of(new StockShowResponse(1L, 100L, 1L, 3)));

        assertThrows(ResponseStatusException.class,
                () -> cartItemService.updateCartItem(1L, new CartItemPatchRequest(10)));
    }

    @Test
    void updateCartItem_happyPath() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);

        CartItem existing = new CartItem();
        existing.setSkuId(100L);
        existing.setQuantity(1);
        when(cartItemRepository.findByIdAndCartId(1L, 1L)).thenReturn(Optional.of(existing));
        when(inventoryGateway.getStocksBySkuIds(List.of(100L)))
                .thenReturn(List.of(new StockShowResponse(1L, 100L, 1L, 10)));
        when(mapper.cartItemShowResponseFrom(existing))
                .thenReturn(new CartItemShowResponse(1L, 100L, 3));

        CartItemShowResponse response = cartItemService.updateCartItem(1L, new CartItemPatchRequest(3));
        assertEquals(3, existing.getQuantity());
    }

    @Test
    void deleteCartItem_notFound_throws() {
        Cart cart = new Cart();
        cart.setId(1L);
        when(cartService.getOrCreateCart()).thenReturn(cart);
        when(cartItemRepository.findByIdAndCartId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> cartItemService.deleteCartItem(99L));
    }
}
