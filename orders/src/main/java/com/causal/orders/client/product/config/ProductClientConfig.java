package com.causal.orders.client.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
public class ProductClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer productGroupConfigurer() {
        return groups -> groups.filterByName("product")
                .forEachClient((group, builder) ->
                        builder.requestInitializer(request -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                                request.getHeaders().setBearerAuth(jwt.getTokenValue());
                            }
                        }));
    }
}
