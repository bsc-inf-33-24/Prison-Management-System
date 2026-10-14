package com.pms.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the Jackson 2 mapper used by the visitor audit services.
 * Spring Boot 4's web stack uses Jackson 3, so it does not expose this
 * Jackson 2 type automatically.
 */
@Configuration
public class JacksonCompatibilityConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
