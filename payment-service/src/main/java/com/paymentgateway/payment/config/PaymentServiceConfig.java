package com.paymentgateway.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Core Spring configuration for Payment Service.
 */
@Configuration
@EnableR2dbcAuditing  // enables @CreatedDate and @LastModifiedDate on entities
public class PaymentServiceConfig {

    /**
     * Configures Jackson for proper Java time serialization.
     *
     * WHY JavaTimeModule?
     * By default Jackson doesn't know how to serialize Java 8+
     * date types like Instant, LocalDateTime etc.
     * JavaTimeModule teaches it to serialize Instant as
     * "2026-04-17T10:30:00Z" instead of throwing an error.
     *
     * WHY WRITE_DATES_AS_TIMESTAMPS = false?
     * Without this, Instant serializes as a Unix timestamp number
     * like 1713345000. With it disabled, you get the readable
     * ISO-8601 string "2026-04-17T10:30:00Z" — much better for
     * debugging Kafka messages and audit logs.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}