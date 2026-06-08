package com.causal.order.worker;

import com.causal.order.config.RabbitMQConfig;
import com.causal.order.dto.request.PaymentWebhookRequest;
import com.causal.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class PaymentProcessor {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    public PaymentProcessor(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, TaskScheduler taskScheduler,
                            @org.springframework.beans.factory.annotation.Value("${services.internal-api-key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl("http://localhost:8080")
                .defaultHeader("X-Internal-Api-Key", apiKey).build();
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler;
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_INITIATE_PAYMENT_QUEUE)
    public void process(Message message) {
        String payload = new String(message.getBody());
        log.info("Received initiate_payment job: {}", payload);

        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode data = node.has("payload") ? node.get("payload") : node;
            Long orderId = data.get("orderId").asLong();

            taskScheduler.schedule(() -> {
                try {
                    log.info("Sending mock payment webhook for order {}", orderId);
                    restClient.post()
                            .uri("/internal/orders/payment/webhook")
                            .body(new PaymentWebhookRequest(orderId, OrderStatus.PAYMENT_SUCCESS))
                            .retrieve()
                            .toBodilessEntity();
                    log.info("Mock payment webhook sent for order {}", orderId);
                } catch (Exception e) {
                    log.error("Failed to send mock payment webhook for order {}", orderId, e);
                }
            }, Instant.now().plus(Duration.ofSeconds(5)));
        } catch (Exception e) {
            log.error("Failed to parse initiate_payment payload", e);
        }
    }
}
