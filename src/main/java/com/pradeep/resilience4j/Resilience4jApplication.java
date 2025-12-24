package com.pradeep.resilience4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application demonstrating Resilience4j patterns
 * applied to a weather service API.
 * 
 * This application showcases:
 * - Circuit Breaker
 * - Retry
 * - Rate Limiter
 * - Bulkhead
 * - Time Limiter
 * - Fallback methods
 * - Micrometer metrics
 */
@SpringBootApplication
public class Resilience4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(Resilience4jApplication.class, args);
    }
}

