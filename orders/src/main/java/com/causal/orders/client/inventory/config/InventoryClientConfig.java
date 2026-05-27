package com.causal.orders.client.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
public class InventoryClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer inventoryGroupConfigurer(
            @Value("${services.inventory.api-key}") String apiKey
    ) {
        return groups -> groups
                .filterByName("inventory")
                .forEachClient((group, builder) -> builder
                        .defaultHeader("X-Internal-Api-Key", apiKey));
    }
}
