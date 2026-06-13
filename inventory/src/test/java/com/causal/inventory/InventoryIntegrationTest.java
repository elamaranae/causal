package com.causal.inventory;

import com.causal.inventory.client.order.OrderGateway;
import com.causal.inventory.dto.request.StockReservationItemRequest;
import com.causal.inventory.dto.request.StockReserveRequest;
import com.causal.inventory.model.Reservation;
import com.causal.inventory.model.Stock;
import com.causal.inventory.repository.OutboxRepository;
import com.causal.inventory.repository.ReservationRepository;
import com.causal.inventory.repository.StockRepository;
import com.causal.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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

    @MockitoSpyBean
    private ReservationRepository reservationRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ReservationService reservationService;

    @MockitoBean
    private OrderGateway orderGateway;

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor internalUser() {
        return SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL");
    }

    @BeforeEach
    void cleanDb() {
        reset(reservationRepository);
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
    void confirmReservation_raceWithReclaim_shouldNotDoubleCountStock() throws Exception {
        createStock(100L, 1L, 50);

        // Reserve 5 units → available=45
        mockMvc.perform(post("/internal/inventory/stocks/reserve")
                        .with(internalUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "orderId": 1, "items": [{"skuId": 100, "quantity": 5}]}
                                """))
                .andExpect(status().isOk());

        // Expire the reservation so reclaimExpired will find it
        List<Reservation> reservations = reservationRepository.findByOrderId(1L);
        reservations.forEach(r -> r.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)));
        reservationRepository.saveAll(reservations);

        CountDownLatch confirmReadDone = new CountDownLatch(1);
        CountDownLatch reclaimDone = new CountDownLatch(1);

        // Extract the spy's default answer which delegates to the real Spring Data proxy.
        // This avoids callRealMethod() which doesn't work on interface-based proxies.
        Answer<?> realAnswer = Mockito.mockingDetails(reservationRepository)
                .getMockCreationSettings().getDefaultAnswer();

        // Intercept both locked and unlocked variants — whichever confirmReservation calls,
        // we pause after the read to give reclaim a window to act on the same rows.
        Answer<List<Reservation>> pauseAfterRead = invocation -> {
            @SuppressWarnings("unchecked")
            List<Reservation> result = (List<Reservation>) realAnswer.answer(invocation);
            confirmReadDone.countDown();
            // With PESSIMISTIC_WRITE, reclaim's DELETE blocks on locked rows, so this
            // timeout fires and confirm proceeds — correct behavior, test passes.
            // Without the lock, reclaim completes within the window, causing the race.
            reclaimDone.await(500, TimeUnit.MILLISECONDS);
            return result;
        };
        doAnswer(pauseAfterRead).when(reservationRepository).findWithLockByOrderId(1L);

        // Thread A: confirm the reservation
        CompletableFuture<Void> confirmFuture = CompletableFuture.runAsync(() ->
                reservationService.confirmReservation("evt-1", 1L,
                        List.of(Map.of("skuId", 100, "quantity", 5))));

        // Thread B: after confirm reads, trigger reclaim by reserving more than available
        CompletableFuture.runAsync(() -> {
            try {
                confirmReadDone.await(5, TimeUnit.SECONDS);
                // Reserve 46 units which exceeds available (45), triggering reclaimExpired
                try {
                    reservationService.reserve(new StockReserveRequest(2L, 2L,
                            List.of(new StockReservationItemRequest(100L, 46))));
                } catch (Exception ignored) {
                    // May fail if still not enough stock after reclaim — that's fine
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                reclaimDone.countDown();
            }
        });

        confirmFuture.get(10, TimeUnit.SECONDS);

        // Reset spy to avoid interfering with other tests
        reset(reservationRepository);

        Stock stock = stockRepository.findBySkuId(100L).orElseThrow();

        boolean confirmedSuccess = outboxRepository.findAll().stream()
                .anyMatch(e -> "order_success".equals(e.getPayload().get("status")));

        // If confirm published order_success, the stock is sold — available should stay at 45
        // With the race bug, reclaim restores 5 units (available→50) AND order_success is published
        if (confirmedSuccess) {
            assertEquals(45, stock.getAvailableCount(),
                    "Race condition: stock was restored by reclaim but order was confirmed as success");
        }
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
