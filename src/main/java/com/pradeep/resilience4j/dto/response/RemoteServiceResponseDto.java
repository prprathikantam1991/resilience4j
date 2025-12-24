package com.pradeep.resilience4j.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for weather service API calls
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteServiceResponseDto {
    // Weather data fields
    private String city;
    private String country;
    private Double temperature;
    private Double humidity;
    private Double pressure;
    private String condition; // sunny, rainy, cloudy, snowy, etc.
    private Double windSpeed;
    private String units; // "C" or "F"
    private String description;
    
    // Resilience tracking fields
    private String resiliencePattern; // Which pattern was applied
    private Boolean fromFallback; // Whether response came from fallback
    private String circuitBreakerState; // Circuit Breaker state: CLOSED, OPEN, HALF_OPEN
    private LocalDateTime timestamp;
    private Long executionTimeMs;
    private String status; // SUCCESS, FALLBACK, ERROR
    
    // Legacy fields for backward compatibility
    @Deprecated
    private String message;
}
