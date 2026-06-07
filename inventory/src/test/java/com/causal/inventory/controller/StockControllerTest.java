package com.causal.inventory.controller;

import com.causal.inventory.config.InternalApiKeyFilter;
import com.causal.inventory.config.SecurityConfig;
import com.causal.inventory.dto.response.ProductStockShowResponse;
import com.causal.inventory.dto.response.ReservationItemResponse;
import com.causal.inventory.dto.response.ReservationResponse;
import com.causal.inventory.dto.response.StockShowResponse;
import com.causal.inventory.service.ReservationService;
import com.causal.inventory.service.StockService;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({StockController.class, ReservationController.class})
@Import({SecurityConfig.class, InternalApiKeyFilter.class})
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private ReservationService reservationService;

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor internalUser() {
        return SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL");
    }

    @Test
    void getStock_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/internal/inventory/stocks/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStock_found_returns200() throws Exception {
        when(stockService.getStock(100L))
                .thenReturn(new StockShowResponse(1L, 100L, 1L, 50));

        mockMvc.perform(get("/internal/inventory/stocks/100").with(internalUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value(100))
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    void getStock_notFound_returns404() throws Exception {
        when(stockService.getStock(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));

        mockMvc.perform(get("/internal/inventory/stocks/999").with(internalUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkStocksBySkuIds_returns200() throws Exception {
        when(stockService.getStocksBySkuIds(List.of(100L, 200L)))
                .thenReturn(List.of(
                        new StockShowResponse(1L, 100L, 1L, 50),
                        new StockShowResponse(2L, 200L, 2L, 30)));

        mockMvc.perform(post("/internal/inventory/stocks/skus/bulk")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuIds": [100, 200]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuId").value(100))
                .andExpect(jsonPath("$[1].skuId").value(200));
    }

    @Test
    void bulkStocksByProductIds_returns200() throws Exception {
        when(stockService.getStocksByProductIds(List.of(1L, 2L)))
                .thenReturn(List.of(
                        new ProductStockShowResponse(1L, true),
                        new ProductStockShowResponse(2L, false)));

        mockMvc.perform(post("/internal/inventory/stocks/products/bulk")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productIds": [1, 2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].available").value(false));
    }

    @Test
    void reserve_validRequest_returns200() throws Exception {
        ReservationResponse response = new ReservationResponse(1L,
                List.of(new ReservationItemResponse(100L, 2, Instant.now().plusSeconds(600))));
        when(reservationService.reserve(any())).thenReturn(response);

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 1,
                                    "orderId": 1,
                                    "items": [{"skuId": 100, "quantity": 2}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.items[0].skuId").value(100));
    }

    @Test
    void reserve_conflict_returns409() throws Exception {
        when(reservationService.reserve(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock"));

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 1,
                                    "orderId": 1,
                                    "items": [{"skuId": 100, "quantity": 999}]
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void extendReservation_returns200() throws Exception {
        ReservationResponse response = new ReservationResponse(1L,
                List.of(new ReservationItemResponse(100L, 2, Instant.now().plusSeconds(300))));
        when(reservationService.extend(any())).thenReturn(response);

        mockMvc.perform(post("/internal/inventory/reservations/extend")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 1,
                                    "orderId": 1,
                                    "items": [{"skuId": 100, "quantity": 2}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));
    }
}
