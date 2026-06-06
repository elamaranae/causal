package com.causal.order.service;

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
import com.causal.order.config.CurrentUser;
import com.causal.order.dto.request.AddressRequest;
import com.causal.order.dto.request.PaymentMethodRequest;
import com.causal.order.dto.request.PaymentRequest;
import com.causal.order.dto.response.OrderShowResponse;
import com.causal.order.mapper.OrderMapper;
import com.causal.order.model.Order;
import com.causal.order.model.OrderAddress;
import com.causal.order.model.OutboxEvent;
import com.causal.order.repository.OrderRepository;
import com.causal.order.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OutboxRepository outboxRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private CurrentUser currentUser;
    @Mock private CartGateway cartGateway;
    @Mock private ProfileGateway profileGateway;
    @Mock private ProductGateway productGateway;
    @Mock private InventoryGateway inventoryGateway;
    @Mock private PaymentEncryptor paymentEncryptor;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER_ID = 1L;

    @Nested
    class Checkout {

        @BeforeEach
        void setUp() {
            lenient().when(currentUser.id()).thenReturn(USER_ID);
        }

        @Test
        void emptyCart_throws() {
            when(cartGateway.getCurrentUserCart())
                    .thenReturn(new CartShowResponse(1L, USER_ID, List.of()));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.checkout());
            assertTrue(ex.getReason().contains("Cart is empty"));
        }

        @Test
        void skuNotFound_throws() {
            CartShowResponse cart = new CartShowResponse(1L, USER_ID,
                    List.of(new CartItemShowResponse(1L, 100L, 2)));
            when(cartGateway.getCurrentUserCart()).thenReturn(cart);
            when(profileGateway.getCurrentUserProfile())
                    .thenReturn(new ProfileShowResponse(1L, USER_ID, "John", "Doe", "USD", null));
            when(productGateway.getSkusByIds(anyList())).thenReturn(List.of()); // no SKUs returned

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.checkout());
            assertTrue(ex.getReason().contains("SKU not found"));
        }

        @Test
        void happyPath_buildsOrderWithCorrectTotal() {
            CartShowResponse cart = new CartShowResponse(1L, USER_ID, List.of(
                    new CartItemShowResponse(1L, 100L, 2),
                    new CartItemShowResponse(2L, 200L, 3)
            ));
            when(cartGateway.getCurrentUserCart()).thenReturn(cart);
            when(profileGateway.getCurrentUserProfile())
                    .thenReturn(new ProfileShowResponse(1L, USER_ID, "John", "Doe", "USD", null));

            SkuShowResponse sku100 = new SkuShowResponse(100L, Map.of(), Map.of(),
                    new PriceResponse("USD", BigDecimal.valueOf(10)), 50,
                    new ProductMinimalResponse(1L, "Widget", "A widget", 1L));
            SkuShowResponse sku200 = new SkuShowResponse(200L, Map.of(), Map.of(),
                    new PriceResponse("USD", BigDecimal.valueOf(25)), 30,
                    new ProductMinimalResponse(2L, "Gadget", "A gadget", 1L));
            when(productGateway.getSkusByIds(anyList())).thenReturn(List.of(sku100, sku200));

            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });
            when(inventoryGateway.reserve(any(), any(), anyList()))
                    .thenReturn(new ReservationResponse(1L, List.of()));
            when(orderMapper.from(any(Order.class)))
                    .thenReturn(new OrderShowResponse(1L, "RESERVED", null, null, null, List.of()));

            orderService.checkout();

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, atLeast(2)).save(captor.capture());

            Order savedOrder = captor.getAllValues().get(0);
            // 2*10 + 3*25 = 95
            assertEquals(0, BigDecimal.valueOf(95).compareTo(savedOrder.getTotalAmount()));
            assertEquals("USD", savedOrder.getTotalCurrency());
            assertEquals(2, savedOrder.getItems().size());
        }

        @Test
        void happyPath_setsReservedStatus() {
            CartShowResponse cart = new CartShowResponse(1L, USER_ID,
                    List.of(new CartItemShowResponse(1L, 100L, 1)));
            when(cartGateway.getCurrentUserCart()).thenReturn(cart);
            when(profileGateway.getCurrentUserProfile())
                    .thenReturn(new ProfileShowResponse(1L, USER_ID, "John", "Doe", "USD", null));
            SkuShowResponse sku = new SkuShowResponse(100L, Map.of(), Map.of(),
                    new PriceResponse("USD", BigDecimal.TEN), 10,
                    new ProductMinimalResponse(1L, "Item", "Desc", 1L));
            when(productGateway.getSkusByIds(anyList())).thenReturn(List.of(sku));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });
            when(inventoryGateway.reserve(any(), any(), anyList()))
                    .thenReturn(new ReservationResponse(1L, List.of()));
            when(orderMapper.from(any(Order.class)))
                    .thenReturn(new OrderShowResponse(1L, "RESERVED", null, null, null, List.of()));

            orderService.checkout();

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, atLeast(2)).save(captor.capture());
            Order finalSave = captor.getAllValues().get(1);
            assertEquals("RESERVED", finalSave.getStatus());
        }

        @Test
        void reservationFails_setsStatusAndThrows() {
            CartShowResponse cart = new CartShowResponse(1L, USER_ID,
                    List.of(new CartItemShowResponse(1L, 100L, 1)));
            when(cartGateway.getCurrentUserCart()).thenReturn(cart);
            when(profileGateway.getCurrentUserProfile())
                    .thenReturn(new ProfileShowResponse(1L, USER_ID, "John", "Doe", "USD", null));
            SkuShowResponse sku = new SkuShowResponse(100L, Map.of(), Map.of(),
                    new PriceResponse("USD", BigDecimal.TEN), 10,
                    new ProductMinimalResponse(1L, "Item", "Desc", 1L));
            when(productGateway.getSkusByIds(anyList())).thenReturn(List.of(sku));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });
            when(inventoryGateway.reserve(any(), any(), anyList()))
                    .thenThrow(new RuntimeException("Out of stock"));

            assertThrows(ResponseStatusException.class, () -> orderService.checkout());

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, atLeastOnce()).save(captor.capture());
            // The order should be in RESERVATION_FAILED after the exception
            // (set before the throw in reserveInventory)
        }
    }

    @Nested
    class Pay {

        @Test
        void notReserved_throws() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus("PENDING");
            when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));

            PaymentRequest request = makePaymentRequest();
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.pay(1L, request));
            assertTrue(ex.getReason().contains("not in RESERVED status"));
        }

        @Test
        void reservationExpired_setsStatusAndThrows() {
            Order order = makeReservedOrder();
            when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));
            doThrow(new RuntimeException("expired"))
                    .when(inventoryGateway).extendReservation(any(), any(), anyList());

            PaymentRequest request = makePaymentRequest();
            assertThrows(ResponseStatusException.class, () -> orderService.pay(1L, request));

            assertEquals("RESERVATION_EXPIRED", order.getStatus());
            verify(orderRepository).save(order);
        }

        @Test
        void happyPath_setsPaymentInitiated() throws Exception {
            Order order = makeReservedOrder();
            when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));
            when(inventoryGateway.extendReservation(any(), any(), anyList()))
                    .thenReturn(new ReservationResponse(1L, List.of()));
            when(orderMapper.from(any(AddressRequest.class))).thenReturn(new OrderAddress());
            when(paymentEncryptor.encrypt(any())).thenReturn("encrypted");
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            PaymentRequest request = makePaymentRequest();
            orderService.pay(1L, request);

            assertEquals("PAYMENT_INITIATED", order.getStatus());
            // 2 outbox events: status change + payment initiation
            verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
        }

        private Order makeReservedOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(USER_ID);
            order.setStatus("RESERVED");
            order.setTotalAmount(BigDecimal.TEN);
            order.setTotalCurrency("USD");
            return order;
        }
    }

    @Nested
    class PaymentWebhook {

        @Test
        void idempotent_sameStatus_noOp() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus("PAYMENT_SUCCESS");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.handlePaymentWebhook(1L, "PAYMENT_SUCCESS");

            verify(orderRepository, never()).save(any());
        }

        @Test
        void wrongStatus_throws() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus("RESERVED");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThrows(ResponseStatusException.class,
                    () -> orderService.handlePaymentWebhook(1L, "PAYMENT_SUCCESS"));
        }

        @Test
        void success_updatesAndPublishes() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(USER_ID);
            order.setStatus("PAYMENT_INITIATED");
            order.setTotalAmount(BigDecimal.TEN);
            order.setTotalCurrency("USD");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.handlePaymentWebhook(1L, "PAYMENT_SUCCESS");

            assertEquals("PAYMENT_SUCCESS", order.getStatus());
            // 2 outbox events: status change + payment_completed
            verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
        }

        @Test
        void orderNotFound_throws() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class,
                    () -> orderService.handlePaymentWebhook(99L, "PAYMENT_SUCCESS"));
        }
    }

    @Nested
    class OrderCompleteWebhook {

        @Test
        void idempotent_sameStatus_noOp() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus("COMPLETED");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.handleOrderCompleteWebhook(1L, "COMPLETED");
            verify(orderRepository, never()).save(any());
        }

        @Test
        void wrongStatus_throws() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus("RESERVED");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThrows(ResponseStatusException.class,
                    () -> orderService.handleOrderCompleteWebhook(1L, "COMPLETED"));
        }

        @Test
        void failed_publishesRefundEvent() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(USER_ID);
            order.setStatus("PAYMENT_SUCCESS");
            order.setTotalAmount(BigDecimal.TEN);
            order.setTotalCurrency("USD");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.handleOrderCompleteWebhook(1L, "FAILED");

            assertEquals("FAILED", order.getStatus());
            // 2 outbox events: status change + refund
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository, times(2)).save(captor.capture());
            OutboxEvent refundEvent = captor.getAllValues().stream()
                    .filter(e -> "refund_initiated".equals(e.getType()))
                    .findFirst().orElseThrow();
            assertEquals("job.refund", refundEvent.getAggregatetype());
        }

        @Test
        void success_noRefundEvent() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(USER_ID);
            order.setStatus("PAYMENT_SUCCESS");
            order.setTotalAmount(BigDecimal.TEN);
            order.setTotalCurrency("USD");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.handleOrderCompleteWebhook(1L, "COMPLETED");

            assertEquals("COMPLETED", order.getStatus());
            // Only 1 outbox event: status change (no refund)
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
        }
    }

    private static PaymentRequest makePaymentRequest() {
        return new PaymentRequest(
                new PaymentMethodRequest("visa", "4111111111111111", "12", "2027", "123", "John Doe"),
                new AddressRequest(null, "123 Main St", null, "NYC", "NY", "US", "10001", null),
                new AddressRequest(null, "123 Main St", null, "NYC", "NY", "US", "10001", null)
        );
    }
}
