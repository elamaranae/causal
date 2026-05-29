package com.causal.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String OUTBOX_EXCHANGE = "outbox";
    private static final String DLX_EXCHANGE = "outbox.dlx";

    public static final String JOB_COMPLETE_ORDER_QUEUE = "inventory.job.complete_order";
    private static final String JOB_COMPLETE_ORDER_DLQ = "inventory.job.complete_order.dlq";

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
    Queue jobCompleteOrderQueue() {
        return QueueBuilder.durable(JOB_COMPLETE_ORDER_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(JOB_COMPLETE_ORDER_DLQ)
                .build();
    }

    // -- dead letter queues --

    @Bean
    Queue jobCompleteOrderDlq() {
        return new Queue(JOB_COMPLETE_ORDER_DLQ, true);
    }

    // -- bindings --

    @Bean
    Binding jobCompleteOrderBinding(Queue jobCompleteOrderQueue, TopicExchange outboxExchange) {
        return BindingBuilder.bind(jobCompleteOrderQueue).to(outboxExchange).with("job.complete_order");
    }

    // -- dead letter bindings --

    @Bean
    Binding jobCompleteOrderDlqBinding(Queue jobCompleteOrderDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(jobCompleteOrderDlq).to(dlxExchange).with(JOB_COMPLETE_ORDER_DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
