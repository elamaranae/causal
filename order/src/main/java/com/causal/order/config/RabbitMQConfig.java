package com.causal.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String OUTBOX_EXCHANGE = "outbox";

    public static final String EVENTS_QUEUE = "order.events";
    public static final String JOB_INITIATE_PAYMENT_QUEUE = "order.job.initiate_payment";

    @Bean
    TopicExchange outboxExchange() {
        return new TopicExchange(OUTBOX_EXCHANGE);
    }

    // -- queues --

    @Bean
    Queue eventsQueue() {
        return new Queue(EVENTS_QUEUE, true);
    }

    @Bean
    Queue jobInitiatePaymentQueue() {
        return new Queue(JOB_INITIATE_PAYMENT_QUEUE, true);
    }

    // -- bindings --

    @Bean
    Binding eventsBinding(Queue eventsQueue, TopicExchange outboxExchange) {
        return BindingBuilder.bind(eventsQueue).to(outboxExchange).with("event.*");
    }

    @Bean
    Binding jobInitiatePaymentBinding(Queue jobInitiatePaymentQueue, TopicExchange outboxExchange) {
        return BindingBuilder.bind(jobInitiatePaymentQueue).to(outboxExchange).with("job.initiate_payment");
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
