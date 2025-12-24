package com.pradeep.resilience4j.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResilienceMetricsDto {
    private Map<String, Object> circuitBreakerMetrics;
    private Map<String, Object> retryMetrics;
    private Map<String, Object> rateLimiterMetrics;
    private Map<String, Object> bulkheadMetrics;
    private Map<String, Object> timeLimiterMetrics;
}
