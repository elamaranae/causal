package com.causal.order.worker;

import com.causal.order.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentProcessor {

    @RabbitListener(queues = RabbitMQConfig.JOB_INITIATE_PAYMENT_QUEUE)
    public void process(String payload) {
        log.info("Received initiate_payment job: {}", payload);
    }
}
