package com.causal.cart.event;

import com.causal.cart.repository.CartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class OrderStatusEventConsumer {

    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;

    public OrderStatusEventConsumer(CartRepository cartRepository, ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order.events", groupId = "cart")
    @Transactional
    public void onOrderEvent(String message) {
        JsonNode root = objectMapper.readTree(message);
        String type = root.has("type") ? root.get("type").asText() : null;

        if (!"event.order_status".equals(type)) {
            return;
        }

        JsonNode payload = root.get("payload");
        String status = payload.has("status") ? payload.get("status").asText() : null;
        if (!"COMPLETED".equals(status)) {
            return;
        }

        Long userId = payload.get("userId").asLong();
        log.info("Order completed for user {}, clearing cart", userId);

        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartRepository.delete(cart);
            log.info("Cart cleared for user {}", userId);
        });
    }
}
