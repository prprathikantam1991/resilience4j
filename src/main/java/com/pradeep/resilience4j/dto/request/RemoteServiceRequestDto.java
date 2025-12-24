package com.pradeep.resilience4j.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for weather service API calls
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteServiceRequestDto {
    private String city;
    private String state; // Optional state/province for more accurate geocoding (e.g., "Massachusetts")
    private String country;
    private String units; // "C" for Celsius, "F" for Fahrenheit
    private String scenario; // success, failure, timeout, slow, random
    private Integer delayMs; // Optional delay in milliseconds
    private Double failureProbability; // Probability of failure (0.0 to 1.0)
    // Legacy field for backward compatibility
    @Deprecated
    private String message;
}
