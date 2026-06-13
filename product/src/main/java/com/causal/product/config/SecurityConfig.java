package com.causal.product.config;

import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
              .requestMatchers("/products/backoffice/**").hasRole("ADMIN")
              .anyRequest().authenticated())
          .oauth2ResourceServer(o -> o
              .bearerTokenResolver(cookieBearerTokenResolver())
              .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
      return http.build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
      JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
      converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
      return converter;
  }

  @Bean
  Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
      return jwt -> {
          List<String> roles = jwt.getClaimAsStringList("roles");
          if (roles == null) return Collections.emptyList();
          return roles.stream()
                  .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                  .collect(Collectors.toList());
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
