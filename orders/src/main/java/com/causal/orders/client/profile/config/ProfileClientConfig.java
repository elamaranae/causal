package com.causal.orders.client.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
public class ProfileClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer profileGroupConfigurer() {
        return groups -> groups.filterByName("profile")
                .forEachClient((group, builder) ->
                        builder.requestInitializer(request -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                                request.getHeaders().setBearerAuth(jwt.getTokenValue());
                            }
                        }));
    }
}
