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
        Long userId = currentUser.id();

        // Gather data from external services
        CartShowResponse cart = fetchCart();
        String currency = profileGateway.getCurrentUserProfile().currency();
        Map<Long, SkuShowResponse> skuMap = fetchSkuMap(cart);

        // Build order with items and total
        Order order = buildOrder(userId, currency, cart, skuMap);
        order = orderRepository.save(order);

        // Reserve inventory (critical section)
        reserveInventory(order);

        order.setStatus("RESERVED");
        order = orderRepository.save(order);

        return orderMapper.from(order);
    }

    public void pay(Long id, PaymentRequest request) {
        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!"RESERVED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not in RESERVED status");
        }

        // Extend reservation while processing payment
        List<StockReservationItemRequest> items = toReservationItems(order);
        try {
            inventoryGateway.extendReservation(order.getUserId(), order.getId(), items);
        } catch (Exception e) {
            order.setStatus("RESERVATION_EXPIRED");
            orderRepository.save(order);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation expired, please checkout again");
        }

        // Set addresses
        order.setShippingAddress(orderMapper.from(request.shippingAddress()));
        order.setBillingAddress(orderMapper.from(request.billingAddress()));

        // TODO: process payment with payment provider

        order.setStatus("PAYMENT_INITIATED");
        orderRepository.save(order);
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

    private Order buildOrder(Long userId, String currency, CartShowResponse cart, Map<Long, SkuShowResponse> skuMap) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("PENDING_RESERVATION");
        order.setTotalCurrency(currency);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemShowResponse cartItem : cart.items()) {
            SkuShowResponse sku = skuMap.get(cartItem.skuId());
            if (sku == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU not found: " + cartItem.skuId());
            }
            OrderItem item = buildOrderItem(order, cartItem, sku);
            order.getItems().add(item);
            total = total.add(sku.price().priceAmount().multiply(BigDecimal.valueOf(cartItem.quantity())));
        }
        order.setTotalAmount(total);

        return order;
    }

    private OrderItem buildOrderItem(Order order, CartItemShowResponse cartItem, SkuShowResponse sku) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setSkuId(cartItem.skuId());
        item.setQuantity(cartItem.quantity());
        item.setSkuName(sku.product().name());
        item.setSkuDescription(sku.product().description());
        item.setPurchaseAmount(sku.price().priceAmount());
        item.setPurchaseCurrency(sku.price().priceCurrency());
        return item;
    }

    private void reserveInventory(Order order) {
        List<StockReservationItemRequest> items = toReservationItems(order);
        try {
            inventoryGateway.reserve(order.getUserId(), order.getId(), items);
        } catch (Exception e) {
            order.setStatus("RESERVATION_FAILED");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to reserve inventory: " + e.getMessage());
        }
    }

    private List<StockReservationItemRequest> toReservationItems(Order order) {
        return order.getItems().stream()
                .map(item -> new StockReservationItemRequest(item.getSkuId(), item.getQuantity()))
                .toList();
    }
}
