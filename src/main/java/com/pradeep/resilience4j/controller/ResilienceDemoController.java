package com.pradeep.resilience4j.controller;

import com.pradeep.resilience4j.dto.response.ApiResponseDto;
import com.pradeep.resilience4j.dto.request.RemoteServiceRequestDto;
import com.pradeep.resilience4j.dto.response.RemoteServiceResponseDto;
import com.pradeep.resilience4j.dto.response.ResilienceMetricsDto;
import com.pradeep.resilience4j.service.ResilienceDemoService;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Service API", description = "Weather service API demonstrating Resilience4j patterns (Circuit Breaker, Retry, Rate Limiter, Bulkhead, Time Limiter) with fallback methods and Micrometer metrics")
@Slf4j
public class ResilienceDemoController {

    private final ResilienceDemoService resilienceDemoService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @PostMapping("/circuit-breaker")
    @Operation(summary = "Get Weather with Circuit Breaker", description = "Fetches weather data with Circuit Breaker protection - opens circuit after failure threshold")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testCircuitBreaker(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            RemoteServiceResponseDto response = resilienceDemoService.callWithCircuitBreaker(request);
            // Include Circuit Breaker state in the response
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("remoteService");
            response.setCircuitBreakerState(cb.getState().name());
            String message = "Weather data retrieved with Circuit Breaker protection";
            if (response.getFromFallback() != null && response.getFromFallback()) {
                message = String.format("Circuit Breaker is %s - Fallback response returned. %s", 
                        cb.getState(), response.getDescription());
            }
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message(message)
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with circuit breaker", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @GetMapping
    @Operation(summary = "Get Weather", description = "Fetches weather data for a city using all Resilience4j patterns")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "C") String units) {
        try {
            RemoteServiceRequestDto request = RemoteServiceRequestDto.builder()
                    .city(city != null ? city : "London")
                    .country(country != null ? country : "UK")
                    .units(units)
                    .scenario("success")
                    .build();
            RemoteServiceResponseDto response = resilienceDemoService.callWithAllPatterns(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/retry")
    @Operation(summary = "Get Weather with Retry", description = "Fetches weather data with Retry protection - automatically retries failed calls")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testRetry(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            RemoteServiceResponseDto response = resilienceDemoService.callWithRetry(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved with Retry protection")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with retry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/rate-limiter")
    @Operation(summary = "Get Weather with Rate Limiter", description = "Fetches weather data with Rate Limiter protection - limits calls per time period")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testRateLimiter(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            RemoteServiceResponseDto response = resilienceDemoService.callWithRateLimiter(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved with Rate Limiter protection")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with rate limiter", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/bulkhead")
    @Operation(summary = "Get Weather with Bulkhead", description = "Fetches weather data with Bulkhead protection - isolates thread pools")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testBulkhead(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            RemoteServiceResponseDto response = resilienceDemoService.callWithBulkhead(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved with Bulkhead protection")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with bulkhead", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/time-limiter")
    @Operation(summary = "Get Weather with Time Limiter", description = "Fetches weather data with Time Limiter protection - enforces maximum execution time")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testTimeLimiter(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            CompletableFuture<RemoteServiceResponseDto> future = resilienceDemoService.callWithTimeLimiter(request);
            RemoteServiceResponseDto response = future.get();
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved with Time Limiter protection")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with time limiter", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/combined")
    @Operation(summary = "Get Weather with Combined Patterns", description = "Fetches weather data with Circuit Breaker + Retry combined")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testCombined(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            RemoteServiceResponseDto response = resilienceDemoService.callWithCircuitBreakerAndRetry(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved with combined patterns")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with combined patterns", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/all-patterns")
    @Operation(summary = "Get Weather with All Patterns", description = "Fetches weather data with all Resilience4j patterns combined (Circuit Breaker + Retry + Rate Limiter + Bulkhead)")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testAllPatterns(
            @RequestBody RemoteServiceRequestDto request) {
        try {
            RemoteServiceResponseDto response = resilienceDemoService.callWithAllPatterns(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather data retrieved with all Resilience4j patterns")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error fetching weather with all patterns", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get Weather Service Resilience4j Metrics", description = "Retrieves metrics for all Resilience4j patterns applied to weather service")
    public ResponseEntity<ApiResponseDto<ResilienceMetricsDto>> getMetrics() {
        try {
            ResilienceMetricsDto metrics = buildMetricsDto();
            return ResponseEntity.ok(ApiResponseDto.<ResilienceMetricsDto>builder()
                    .success(true)
                    .message("Weather service Resilience4j metrics retrieved successfully")
                    .data(metrics)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving weather service metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<ResilienceMetricsDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/success")
    @Operation(summary = "Test Successful Weather Call", description = "Simulates a successful weather API call")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testSuccess(
            @RequestBody(required = false) RemoteServiceRequestDto request) {
        try {
            if (request == null) {
                request = RemoteServiceRequestDto.builder()
                        .city("London")
                        .country("UK")
                        .units("C")
                        .scenario("success")
                        .build();
            }
            RemoteServiceResponseDto response = resilienceDemoService.callWithCircuitBreaker(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Successful weather call test completed")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error in success test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/failure")
    @Operation(summary = "Test Weather API Failure Scenario", description = "Simulates a weather API failure scenario to trigger resilience patterns")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testFailure(
            @RequestBody(required = false) RemoteServiceRequestDto request) {
        try {
            if (request == null) {
                request = RemoteServiceRequestDto.builder()
                        .city("London")
                        .country("UK")
                        .units("C")
                        .scenario("failure")
                        .build();
            }
            RemoteServiceResponseDto response = resilienceDemoService.callWithCircuitBreaker(request);
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather API failure scenario test completed")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error in failure test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/timeout")
    @Operation(summary = "Test Weather API Timeout Scenario", description = "Simulates a weather API timeout scenario")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testTimeout(
            @RequestBody(required = false) RemoteServiceRequestDto request) {
        try {
            if (request == null) {
                request = RemoteServiceRequestDto.builder()
                        .city("London")
                        .country("UK")
                        .units("C")
                        .scenario("timeout")
                        .build();
            }
            CompletableFuture<RemoteServiceResponseDto> future = resilienceDemoService.callWithTimeLimiter(request);
            RemoteServiceResponseDto response = future.get();
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Weather API timeout scenario test completed")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error in timeout test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PostMapping("/slow")
    @Operation(summary = "Test Slow Weather API Response", description = "Simulates a slow weather API response scenario")
    public ResponseEntity<ApiResponseDto<RemoteServiceResponseDto>> testSlow(
            @RequestBody(required = false) RemoteServiceRequestDto request) {
        try {
            if (request == null) {
                request = RemoteServiceRequestDto.builder()
                        .city("London")
                        .country("UK")
                        .units("C")
                        .scenario("slow")
                        .delayMs(4000)
                        .build();
            }
            CompletableFuture<RemoteServiceResponseDto> future = resilienceDemoService.callWithTimeLimiter(request);
            RemoteServiceResponseDto response = future.get();
            return ResponseEntity.ok(ApiResponseDto.<RemoteServiceResponseDto>builder()
                    .success(true)
                    .message("Slow weather API response test completed")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error in slow response test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.<RemoteServiceResponseDto>builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    private ResilienceMetricsDto buildMetricsDto() {
        Map<String, Object> circuitBreakerMetrics = new HashMap<>();
        Map<String, Object> retryMetrics = new HashMap<>();
        Map<String, Object> rateLimiterMetrics = new HashMap<>();
        Map<String, Object> bulkheadMetrics = new HashMap<>();
        Map<String, Object> timeLimiterMetrics = new HashMap<>();

        // Circuit Breaker Metrics for Weather Service
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("remoteService");
            circuitBreakerMetrics.put("state", circuitBreaker.getState().name());
            circuitBreakerMetrics.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
            circuitBreakerMetrics.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
            circuitBreakerMetrics.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            circuitBreakerMetrics.put("numberOfNotPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
        } catch (Exception e) {
            circuitBreakerMetrics.put("error", "Metrics not available: " + e.getMessage());
        }

        // Retry Metrics for Weather Service
        try {
            Retry retry = retryRegistry.retry("remoteService");
            retryMetrics.put("numberOfSuccessfulCallsWithoutRetryAttempt", retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryMetrics.put("numberOfSuccessfulCallsWithRetryAttempt", retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryMetrics.put("numberOfFailedCallsWithoutRetryAttempt", retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            retryMetrics.put("numberOfFailedCallsWithRetryAttempt", retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
        } catch (Exception e) {
            retryMetrics.put("error", "Metrics not available: " + e.getMessage());
        }

        // Rate Limiter Metrics for Weather Service
        try {
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("remoteService");
            rateLimiterMetrics.put("availablePermissions", rateLimiter.getMetrics().getAvailablePermissions());
            rateLimiterMetrics.put("numberOfWaitingThreads", rateLimiter.getMetrics().getNumberOfWaitingThreads());
        } catch (Exception e) {
            rateLimiterMetrics.put("error", "Metrics not available: " + e.getMessage());
        }

        // Bulkhead Metrics for Weather Service
        try {
            Bulkhead bulkhead = bulkheadRegistry.bulkhead("remoteService");
            bulkheadMetrics.put("availableConcurrentCalls", bulkhead.getMetrics().getAvailableConcurrentCalls());
            bulkheadMetrics.put("maxAllowedConcurrentCalls", bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        } catch (Exception e) {
            bulkheadMetrics.put("error", "Metrics not available: " + e.getMessage());
        }

        // Time Limiter Metrics for Weather Service
        try {
            TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("remoteService");
            // TimeLimiter doesn't have extensive metrics in the same way, but we can note it's configured
            timeLimiterMetrics.put("configured", true);
        } catch (Exception e) {
            timeLimiterMetrics.put("error", "Metrics not available: " + e.getMessage());
        }

        return ResilienceMetricsDto.builder()
                .circuitBreakerMetrics(circuitBreakerMetrics)
                .retryMetrics(retryMetrics)
                .rateLimiterMetrics(rateLimiterMetrics)
                .bulkheadMetrics(bulkheadMetrics)
                .timeLimiterMetrics(timeLimiterMetrics)
                .build();
    }
}
