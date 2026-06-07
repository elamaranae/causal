package com.causal.inventory;

import com.causal.inventory.client.order.OrderGateway;
import com.causal.inventory.model.Stock;
import com.causal.inventory.repository.OutboxRepository;
import com.causal.inventory.repository.ReservationRepository;
import com.causal.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ImportTestcontainers
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private OrderGateway orderGateway;

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor internalUser() {
        return SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL");
    }

    @BeforeEach
    void cleanDb() {
        outboxRepository.deleteAll();
        reservationRepository.deleteAll();
        stockRepository.deleteAll();
    }

    private Stock createStock(Long skuId, Long productId, int quantity) {
        Stock stock = new Stock();
        stock.setSkuId(skuId);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setAvailableCount(quantity);
        return stockRepository.save(stock);
    }

    @Test
    void getStock_returnsRealDbData() throws Exception {
        createStock(100L, 1L, 50);

        mockMvc.perform(get("/internal/inventory/stocks/100").with(internalUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value(100))
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    void getStock_notFound_returns404() throws Exception {
        mockMvc.perform(get("/internal/inventory/stocks/999").with(internalUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserve_decrementsAvailableInDb() throws Exception {
        createStock(100L, 1L, 50);

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 1,
                                    "orderId": 1,
                                    "items": [{"skuId": 100, "quantity": 5}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.items[0].skuId").value(100));

        Stock stock = stockRepository.findBySkuId(100L).orElseThrow();
        assertEquals(45, stock.getAvailableCount());
        assertEquals(50, stock.getQuantity());

        assertEquals(1, reservationRepository.findByOrderId(1L).size());
    }

    @Test
    void reserve_insufficientStock_returns409() throws Exception {
        createStock(100L, 1L, 3);

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 1,
                                    "orderId": 1,
                                    "items": [{"skuId": 100, "quantity": 10}]
                                }
                                """))
                .andExpect(status().isConflict());

        Stock stock = stockRepository.findBySkuId(100L).orElseThrow();
        assertEquals(3, stock.getAvailableCount());

        assertTrue(reservationRepository.findByOrderId(1L).isEmpty());
    }

    @Test
    void reserve_idempotent_sameRequest() throws Exception {
        createStock(100L, 1L, 50);

        String body = """
                {
                    "userId": 1,
                    "orderId": 1,
                    "items": [{"skuId": 100, "quantity": 5}]
                }
                """;

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        Stock stock = stockRepository.findBySkuId(100L).orElseThrow();
        assertEquals(45, stock.getAvailableCount());
    }

    @Test
    void reserve_thenExtend_updatesExpiry() throws Exception {
        createStock(100L, 1L, 50);

        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "orderId": 1, "items": [{"skuId": 100, "quantity": 5}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/inventory/reservations/extend")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "orderId": 1, "items": [{"skuId": 100, "quantity": 5}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    void bulkStocksBySkuIds_returnsMultiple() throws Exception {
        createStock(100L, 1L, 50);
        createStock(200L, 2L, 30);

        mockMvc.perform(post("/internal/inventory/stocks/skus/bulk")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuIds": [100, 200]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
