package com.causal.inventory.client.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
public class OrderClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer orderGroupConfigurer(
            @Value("${services.internal-api-key}") String apiKey
    ) {
        return groups -> groups
                .filterByName("order")
                .forEachClient((group, builder) -> builder
                        .defaultHeader("X-Internal-Api-Key", apiKey));
    }
}
