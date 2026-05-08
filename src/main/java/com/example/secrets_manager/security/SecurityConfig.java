package com.example.secrets_manager.security;

import com.example.secrets_manager.tracing.CorrelationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthEntryPoint unauthorizedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CorrelationFilter correlationFilter;
  private final CryptoAuthenticationProvider cryptoAuthenticationProvider;

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable) // CSRF is not needed for stateless REST APIs
        .exceptionHandling(
            exception ->
                exception.authenticationEntryPoint(
                    unauthorizedHandler)) // Handle unauthorized requests
        .sessionManagement(
            session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS)) // Use stateless sessions for JWT
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS)
                    .permitAll() // Public endpoints
                    .anyRequest()
                    .authenticated() // All other requests require authentication
            )
        .authenticationProvider(cryptoAuthenticationProvider); // Register custom provider

    // CorrelationFilter must run very early to ensure tracing is active for all subsequent filters
    http.addFilterBefore(correlationFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
