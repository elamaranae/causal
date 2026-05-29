package com.causal.inventory.worker;

import com.causal.inventory.client.order.OrderGateway;
import com.causal.inventory.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class OrderCompleter {

    private final OrderGateway orderGateway;
    private final ObjectMapper objectMapper;

    public OrderCompleter(OrderGateway orderGateway, ObjectMapper objectMapper) {
        this.orderGateway = orderGateway;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_COMPLETE_ORDER_QUEUE)
    public void process(Message message) {
        String body = new String(message.getBody());
        log.info("Received complete_order job: {}", body);

        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode data = node.has("payload") ? node.get("payload") : node;
            Long orderId = data.get("orderId").asLong();
            String status = data.get("status").asText();

            String orderStatus = "order_success".equals(status) ? "COMPLETED" : "FAILED";

            log.info("Completing order {} with status {}", orderId, orderStatus);
            orderGateway.completeOrder(orderId, orderStatus);
            log.info("Order {} completed with status {}", orderId, orderStatus);
        } catch (Exception e) {
            log.error("Failed to complete order", e);
            throw new RuntimeException("Failed to complete order", e);
        }
    }
}
