package com.causal.order.controller;

import com.causal.order.config.InternalApiKeyFilter;
import com.causal.order.config.SecurityConfig;
import com.causal.order.dto.response.*;
import com.causal.order.model.DeliveryStatus;
import com.causal.order.model.OrderStatus;
import com.causal.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, InternalApiKeyFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("1").claim("email", "test@test.com"));
    }

    @Test
    void getOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrders_authenticated_returns200() throws Exception {
        OrderListResponse response = new OrderListResponse(List.of(), 0, 10, 0, 0);
        when(orderService.getOrders(0, 10)).thenReturn(response);

        mockMvc.perform(get("/orders").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getOrders_withPagination() throws Exception {
        OrderListResponse response = new OrderListResponse(List.of(), 2, 5, 50, 10);
        when(orderService.getOrders(2, 5)).thenReturn(response);

        mockMvc.perform(get("/orders?page=2&size=5").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(10));
    }

    @Test
    void getOrder_found_returns200() throws Exception {
        PriceResponse total = new PriceResponse("USD", BigDecimal.valueOf(95));
        OrderShowResponse order = new OrderShowResponse(1L, OrderStatus.RESERVED, total, null, null,
                List.of(new OrderItemShowResponse(1L, 100L, 2, "Widget", "A widget", DeliveryStatus.PENDING,
                        new PriceResponse("USD", BigDecimal.TEN))));
        when(orderService.getOrder(1L)).thenReturn(order);

        mockMvc.perform(get("/orders/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.total.priceAmount").value(95))
                .andExpect(jsonPath("$.items[0].skuName").value("Widget"));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        when(orderService.getOrder(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        mockMvc.perform(get("/orders/99").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderStatus_returns200() throws Exception {
        when(orderService.getOrderStatus(1L))
                .thenReturn(new OrderStatusResponse(1L, OrderStatus.PAYMENT_INITIATED));

        mockMvc.perform(get("/orders/1/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_INITIATED"));
    }

    @Test
    void checkout_authenticated_returns200() throws Exception {
        OrderShowResponse order = new OrderShowResponse(1L, OrderStatus.RESERVED, null, null, null, List.of());
        when(orderService.checkout()).thenReturn(order);

        mockMvc.perform(post("/orders/checkout").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void checkout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/orders/checkout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pay_validRequest_returns200() throws Exception {
        String body = """
                {
                    "paymentMethod": {
                        "type": "visa",
                        "cardNumber": "4111111111111111",
                        "expiryMonth": "12",
                        "expiryYear": "2027",
                        "cvv": "123",
                        "cardholderName": "John Doe"
                    },
                    "shippingAddress": {
                        "line1": "123 Main St",
                        "city": "NYC",
                        "state": "NY",
                        "country": "US",
                        "pincode": "10001"
                    },
                    "billingAddress": {
                        "line1": "123 Main St",
                        "city": "NYC",
                        "state": "NY",
                        "country": "US",
                        "pincode": "10001"
                    }
                }
                """;

        mockMvc.perform(post("/orders/1/pay")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(orderService).pay(eq(1L), any());
    }

    @Test
    void paymentWebhook_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/internal/orders/payment/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "status": "PAYMENT_SUCCESS"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentWebhook_withJwt_returns403() throws Exception {
        mockMvc.perform(post("/internal/orders/payment/webhook")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "status": "PAYMENT_SUCCESS"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentWebhook_withApiKey_returns200() throws Exception {
        mockMvc.perform(post("/internal/orders/payment/webhook")
                        .with(SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "status": "PAYMENT_SUCCESS"}
                                """))
                .andExpect(status().isOk());

        verify(orderService).handlePaymentWebhook(1L, OrderStatus.PAYMENT_SUCCESS);
    }

    @Test
    void completeWebhook_withApiKey_returns200() throws Exception {
        mockMvc.perform(post("/internal/orders/complete/webhook")
                        .with(SecurityMockMvcRequestPostProcessors.user("internal-service").roles("INTERNAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": 1, "status": "COMPLETED"}
                                """))
                .andExpect(status().isOk());

        verify(orderService).handleOrderCompleteWebhook(1L, OrderStatus.COMPLETED);
    }
}
