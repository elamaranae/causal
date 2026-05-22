package com.causal.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${services.internal-api-key}")
  private String internalApiKey;

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
              .jwt(Customizer.withDefaults()))
          .addFilterBefore(internalApiKeyFilter(), BearerTokenAuthenticationFilter.class);
      return http.build();
  }

  private OncePerRequestFilter internalApiKeyFilter() {
      return new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                  throws ServletException, IOException {
              String key = request.getHeader("X-Internal-Api-Key");
              if (key != null && key.equals(internalApiKey)) {
                  SecurityContextHolder.getContext().setAuthentication(
                          new UsernamePasswordAuthenticationToken("internal-service", null, List.of()));
              }
              filterChain.doFilter(request, response);
          }
      };
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
