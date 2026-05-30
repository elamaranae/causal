package com.causal.inventory.event;

import com.causal.inventory.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderEventConsumer {

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order.events", groupId = "inventory")
    public void onOrderEvent(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String eventId = payload.has("eventId") ? payload.get("eventId").asText() : null;
            String status = payload.get("status").asText();
            Long orderId = payload.get("orderId").asLong();
            List<Map<String, Object>> items = parseItems(payload.get("items"));

            log.info("Received order event {} for order {} with status {}", eventId, orderId, status);

            switch (status) {
                case "PAYMENT_SUCCESS" -> {
                    reservationService.confirmReservation(eventId, orderId, items);
                    log.info("Confirmed reservation for order {}", orderId);
                }
                case "PAYMENT_FAILED" -> {
                    reservationService.releaseReservation(eventId, orderId, items);
                    log.info("Released reservation for order {}", orderId);
                }
                default -> log.warn("Unknown payment status: {}", status);
            }
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    private List<Map<String, Object>> parseItems(JsonNode itemsNode) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                items.add(Map.of(
                        "skuId", item.get("skuId").asLong(),
                        "quantity", item.get("quantity").asInt()
                ));
            }
        }
        return items;
    }
}
