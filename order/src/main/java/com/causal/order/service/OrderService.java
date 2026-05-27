package com.causal.order.service;

import com.causal.order.client.cart.CartGateway;
import com.causal.order.client.cart.dto.response.CartItemShowResponse;
import com.causal.order.client.cart.dto.response.CartShowResponse;
import com.causal.order.client.inventory.InventoryGateway;
import com.causal.order.client.inventory.dto.request.StockReservationItemRequest;
import com.causal.order.client.inventory.dto.response.ReservationItemResponse;
import com.causal.order.client.inventory.dto.response.ReservationResponse;
import com.causal.order.client.product.ProductGateway;
import com.causal.order.client.product.dto.response.SkuShowResponse;
import com.causal.order.client.profile.ProfileGateway;
import com.causal.order.config.CurrentUser;
import com.causal.order.dto.request.PaymentRequest;
import com.causal.order.dto.response.OrderShowResponse;
import com.causal.order.dto.response.OrderStatusResponse;
import com.causal.order.mapper.OrderMapper;
import com.causal.order.model.Order;
import com.causal.order.model.OrderItem;
import com.causal.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CurrentUser currentUser;
    private final CartGateway cartGateway;
    private final ProfileGateway profileGateway;
    private final ProductGateway productGateway;
    private final InventoryGateway inventoryGateway;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        CurrentUser currentUser,
                        CartGateway cartGateway,
                        ProfileGateway profileGateway,
                        ProductGateway productGateway,
                        InventoryGateway inventoryGateway) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.currentUser = currentUser;
        this.cartGateway = cartGateway;
        this.profileGateway = profileGateway;
        this.productGateway = productGateway;
        this.inventoryGateway = inventoryGateway;
    }

    public OrderStatusResponse getOrderStatus(Long id) {
        return orderRepository.findStatusById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public OrderShowResponse getOrder(Long id) {
        return orderRepository.findDetailById(id)
                .map(orderMapper::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Transactional
    public OrderShowResponse checkout() {
        Order order = createPendingOrder();

        CartShowResponse cart = fetchCart();
        String currency = profileGateway.getCurrentUserProfile().currency();
        Map<Long, SkuShowResponse> skuMap = fetchSkuMap(cart);

        ReservationResponse reservation = reserveInventory(order, cart);

        buildOrderItems(order, reservation, skuMap);
        finalizeOrder(order, currency);

        return orderMapper.from(order);
    }

    private Order createPendingOrder() {
        Order order = new Order();
        order.setUserId(currentUser.id());
        order.setStatus("PENDING_RESERVATION");
        return orderRepository.save(order);
    }

    private CartShowResponse fetchCart() {
        CartShowResponse cart = cartGateway.getCurrentUserCart();
        if (cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }
        return cart;
    }

    private Map<Long, SkuShowResponse> fetchSkuMap(CartShowResponse cart) {
        List<Long> skuIds = cart.items().stream().map(CartItemShowResponse::skuId).toList();
        return productGateway.getSkusByIds(skuIds).stream()
                .collect(Collectors.toMap(SkuShowResponse::id, Function.identity()));
    }

    private ReservationResponse reserveInventory(Order order, CartShowResponse cart) {
        List<StockReservationItemRequest> items = cart.items().stream()
                .map(item -> new StockReservationItemRequest(item.skuId(), item.quantity()))
                .toList();
        try {
            return inventoryGateway.reserve(order.getUserId(), order.getId(), items);
        } catch (Exception e) {
            order.setStatus("FAILED");
            orderRepository.save(order);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to reserve inventory: " + e.getMessage());
        }
    }

    private void buildOrderItems(Order order, ReservationResponse reservation, Map<Long, SkuShowResponse> skuMap) {
        for (ReservationItemResponse reserved : reservation.items()) {
            SkuShowResponse sku = skuMap.get(reserved.skuId());
            if (sku == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU not found: " + reserved.skuId());
            }
            order.getItems().add(buildOrderItem(order, reserved, sku));
        }
    }

    private OrderItem buildOrderItem(Order order, ReservationItemResponse reserved, SkuShowResponse sku) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setSkuId(reserved.skuId());
        item.setQuantity(reserved.quantity());
        item.setSkuName(sku.product().name());
        item.setSkuDescription(sku.product().description());
        item.setPurchaseAmount(sku.price().priceAmount());
        item.setPurchaseCurrency(sku.price().priceCurrency());
        return item;
    }

    private void finalizeOrder(Order order, String currency) {
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getPurchaseAmount().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        order.setTotalCurrency(currency);
        order.setStatus("RESERVED");
        orderRepository.save(order);
    }

    public void pay(Long id, PaymentRequest request) {
        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!"RESERVED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not in RESERVED status");
        }

        // Extend reservation while processing payment
        List<StockReservationItemRequest> items = order.getItems().stream()
                .map(item -> new StockReservationItemRequest(item.getSkuId(), item.getQuantity()))
                .toList();
        try {
            inventoryGateway.extendReservation(order.getUserId(), order.getId(), items);
        } catch (Exception e) {
            order.setStatus("RESERVATION_EXPIRED");
            orderRepository.save(order);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation expired, please checkout again");
        }

        // TODO: process payment with payment provider

        order.setStatus("PAYMENT_INITIATED");
        orderRepository.save(order);
    }
}
