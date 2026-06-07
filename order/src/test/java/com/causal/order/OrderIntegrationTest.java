package com.causal.order;

import com.causal.order.client.cart.CartGateway;
import com.causal.order.client.cart.dto.response.CartItemShowResponse;
import com.causal.order.client.cart.dto.response.CartShowResponse;
import com.causal.order.client.inventory.InventoryGateway;
import com.causal.order.client.inventory.dto.response.ReservationResponse;
import com.causal.order.client.product.ProductGateway;
import com.causal.order.client.product.dto.response.PriceResponse;
import com.causal.order.client.product.dto.response.ProductMinimalResponse;
import com.causal.order.client.product.dto.response.SkuShowResponse;
import com.causal.order.client.profile.ProfileGateway;
import com.causal.order.client.profile.dto.response.ProfileShowResponse;
import com.causal.order.model.Order;
import com.causal.order.model.OrderStatus;
import com.causal.order.repository.OrderRepository;
import com.causal.order.repository.OutboxRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ImportTestcontainers
@ActiveProfiles("test")
class OrderIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private CartGateway cartGateway;

    @MockitoBean
    private ProductGateway productGateway;

    @MockitoBean
    private InventoryGateway inventoryGateway;

    @MockitoBean
    private ProfileGateway profileGateway;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("1").claim("email", "test@test.com"));
    }

    @BeforeEach
    void cleanDb() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private void setupCheckoutMocks() {
        CartShowResponse cart = new CartShowResponse(1L, 1L,
                List.of(new CartItemShowResponse(1L, 100L, 1)));
        when(cartGateway.getCurrentUserCart()).thenReturn(cart);
        when(profileGateway.getCurrentUserProfile())
                .thenReturn(new ProfileShowResponse(1L, 1L, "John", "Doe", "USD", null));
        SkuShowResponse sku = new SkuShowResponse(100L, Map.of(), Map.of(),
                new PriceResponse("USD", BigDecimal.TEN), 10,
                new ProductMinimalResponse(1L, "Item", "Desc", 1L));
        when(productGateway.getSkusByIds(anyList())).thenReturn(List.of(sku));
        when(inventoryGateway.reserve(any(), any(), anyList()))
                .thenReturn(new ReservationResponse(1L, List.of()));
        when(inventoryGateway.extendReservation(any(), any(), anyList()))
                .thenReturn(new ReservationResponse(1L, List.of()));
    }

    @Test
    void checkout_fullFlow_persistsOrderInDb() throws Exception {
        CartShowResponse cart = new CartShowResponse(1L, 1L, List.of(
                new CartItemShowResponse(1L, 100L, 2),
                new CartItemShowResponse(2L, 200L, 1)
        ));
        when(cartGateway.getCurrentUserCart()).thenReturn(cart);
        when(profileGateway.getCurrentUserProfile())
                .thenReturn(new ProfileShowResponse(1L, 1L, "John", "Doe", "USD", null));

        SkuShowResponse sku100 = new SkuShowResponse(100L, Map.of(), Map.of(),
                new PriceResponse("USD", new BigDecimal("29.99")), 50,
                new ProductMinimalResponse(1L, "Widget", "A widget", 1L));
        SkuShowResponse sku200 = new SkuShowResponse(200L, Map.of(), Map.of(),
                new PriceResponse("USD", new BigDecimal("49.99")), 30,
                new ProductMinimalResponse(2L, "Gadget", "A gadget", 1L));
        when(productGateway.getSkusByIds(anyList())).thenReturn(List.of(sku100, sku200));
        when(inventoryGateway.reserve(any(), any(), anyList()))
                .thenReturn(new ReservationResponse(1L, List.of()));

        mockMvc.perform(post("/orders/checkout").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.items.length()").value(2));

        List<Order> orders = orderRepository.findByUserId(1L);
        assertEquals(1, orders.size());
        Order order = orders.get(0);
        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals(0, new BigDecimal("109.97").compareTo(order.getTotalAmount()));
        assertEquals("USD", order.getTotalCurrency());

        assertFalse(outboxRepository.findAll().isEmpty());
    }

    @Test
    void checkout_thenPay_statusProgression() throws Exception {
        setupCheckoutMocks();

        mockMvc.perform(post("/orders/checkout").with(jwt()))
                .andExpect(status().isOk());

        long orderId = orderRepository.findByUserId(1L).get(0).getId();

        String payBody = """
                {
                    "paymentMethod": {"type":"visa","cardNumber":"4111111111111111","expiryMonth":"12","expiryYear":"2027","cvv":"123","cardholderName":"John Doe"},
                    "shippingAddress": {"line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"},
                    "billingAddress": {"line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"}
                }
                """;
        mockMvc.perform(post("/orders/" + orderId + "/pay")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk());

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.PAYMENT_INITIATED, order.getStatus());
    }

    @Test
    void paymentWebhook_thenCompleteWebhook_fullLifecycle() throws Exception {
        setupCheckoutMocks();

        mockMvc.perform(post("/orders/checkout").with(jwt()))
                .andExpect(status().isOk());

        long orderId = orderRepository.findByUserId(1L).get(0).getId();

        String payBody = """
                {
                    "paymentMethod": {"type":"visa","cardNumber":"4111111111111111","expiryMonth":"12","expiryYear":"2027","cvv":"123","cardholderName":"John Doe"},
                    "shippingAddress": {"line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"},
                    "billingAddress": {"line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"}
                }
                """;
        mockMvc.perform(post("/orders/" + orderId + "/pay")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON).content(payBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/orders/payment/webhook")
                        .with(SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"status\":\"PAYMENT_SUCCESS\"}"))
                .andExpect(status().isOk());

        assertEquals(OrderStatus.PAYMENT_SUCCESS, orderRepository.findById(orderId).orElseThrow().getStatus());

        mockMvc.perform(post("/internal/orders/complete/webhook")
                        .with(SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        assertEquals(OrderStatus.COMPLETED, orderRepository.findById(orderId).orElseThrow().getStatus());

        mockMvc.perform(get("/orders").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        mockMvc.perform(get("/orders/99999").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
