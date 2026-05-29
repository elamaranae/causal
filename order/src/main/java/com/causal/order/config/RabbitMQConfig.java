package com.causal.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String OUTBOX_EXCHANGE = "outbox";
    private static final String DLX_EXCHANGE = "outbox.dlx";

    public static final String EVENTS_QUEUE = "order.events";
    public static final String JOB_INITIATE_PAYMENT_QUEUE = "order.job.initiate_payment";
    private static final String EVENTS_DLQ = "order.events.dlq";
    private static final String JOB_INITIATE_PAYMENT_DLQ = "order.job.initiate_payment.dlq";

    @Bean
    TopicExchange outboxExchange() {
        return new TopicExchange(OUTBOX_EXCHANGE);
    }

    @Bean
    DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // -- queues --

    @Bean
    Queue eventsQueue() {
        return QueueBuilder.durable(EVENTS_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(EVENTS_DLQ)
                .build();
    }

    @Bean
    Queue jobInitiatePaymentQueue() {
        return QueueBuilder.durable(JOB_INITIATE_PAYMENT_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(JOB_INITIATE_PAYMENT_DLQ)
                .build();
    }

    // -- dead letter queues --

    @Bean
    Queue eventsDlq() {
        return new Queue(EVENTS_DLQ, true);
    }

    @Bean
    Queue jobInitiatePaymentDlq() {
        return new Queue(JOB_INITIATE_PAYMENT_DLQ, true);
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

    // -- dead letter bindings --

    @Bean
    Binding eventsDlqBinding(Queue eventsDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(eventsDlq).to(dlxExchange).with(EVENTS_DLQ);
    }

    @Bean
    Binding jobInitiatePaymentDlqBinding(Queue jobInitiatePaymentDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(jobInitiatePaymentDlq).to(dlxExchange).with(JOB_INITIATE_PAYMENT_DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
