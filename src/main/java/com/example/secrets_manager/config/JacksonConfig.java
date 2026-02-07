package com.example.secrets_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Provides a Spring-managed ObjectMapper bean configured with JavaTimeModule.
     * This ensures that Java 8 Date and Time API types (like Instant) are
     * correctly serialized and deserialized as part of JSON operations.
     * This ObjectMapper will be injected wherever an ObjectMapper dependency is declared,
     * such as in CryptographyServiceImpl.
     *
     * @return A configured ObjectMapper instance.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
