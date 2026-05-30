package com.causal.inventory.client.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
public class OrderClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer orderGroupConfigurer() {
        return groups -> groups
                .filterByName("order")
                .forEachClient((group, builder) -> {});
    }
}
