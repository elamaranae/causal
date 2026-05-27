package com.causal.order.client.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
public class ProductClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer productGroupConfigurer() {
        return groups -> groups
                .filterByName("product")
                .forEachClient((group, builder) -> builder
                        .requestInitializer(request -> {
                            Object principal = SecurityContextHolder.getContext()
                                    .getAuthentication().getPrincipal();
                            if (principal instanceof Jwt jwt) {
                                request.getHeaders().setBearerAuth(jwt.getTokenValue());
                            }
                        }));
    }
}
