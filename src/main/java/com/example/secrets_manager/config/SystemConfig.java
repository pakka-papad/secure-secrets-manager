package com.example.secrets_manager.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemConfig {

  /** Provides a UTC-based system clock for consistent time across nodes. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
