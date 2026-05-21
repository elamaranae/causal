package com.causal.cart.config;

import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
      http
          .sessionManagement(session -> session
              .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a -> a
              .requestMatchers("/actuator/**").permitAll()
              .anyRequest().authenticated())
          .oauth2ResourceServer(o -> o
              .bearerTokenResolver(cookieBearerTokenResolver())
              .jwt(Customizer.withDefaults()));
      return http.build();
  }

  @Bean
  BearerTokenResolver cookieBearerTokenResolver() {
      return request -> {
          Cookie[] cookies = request.getCookies();
          if (cookies != null) {
              for (Cookie cookie : cookies) {
                  if ("access_token".equals(cookie.getName())) {
                      return cookie.getValue();
                  }
              }
          }
          String authHeader = request.getHeader("Authorization");
          if (authHeader != null && authHeader.startsWith("Bearer ")) {
              return authHeader.substring(7);
          }
          return null;
      };
  }
}
