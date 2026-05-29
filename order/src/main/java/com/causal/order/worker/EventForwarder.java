package com.causal.order.worker;

import com.causal.order.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Slf4j
@Component
public class EventForwarder {

    public static final String TOPIC = "order.events";
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventForwarder(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.EVENTS_QUEUE)
    public void forward(Message message) {
        String body = new String(message.getBody());
        log.info("Forwarding event to Kafka: {}", body);

        JsonNode node = objectMapper.readTree(body);
        JsonNode payload = node.has("payload") ? node.get("payload") : node;
        String key = payload.has("orderId") ? payload.get("orderId").asText() : null;

        try {
            var result = kafkaTemplate.send(TOPIC, key, payload.toString()).get(SEND_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            log.info("Forwarded event to Kafka topic {} partition {}",
                    TOPIC, result.getRecordMetadata().partition());
        } catch (Exception e) {
            log.error("Failed to forward event to Kafka, message will be requeued: {}", e.getMessage());
            throw new RuntimeException("Failed to forward event to Kafka", e);
        }
    }
}
