package com.causal.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
      http
          .authorizeHttpRequests(a -> a.anyRequest().authenticated())
          .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
      return http.build();
  }
}
