package com.pradeep.resilience4j.service;

import com.pradeep.resilience4j.dto.request.RemoteServiceRequestDto;
import com.pradeep.resilience4j.dto.response.RemoteServiceResponseDto;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Service demonstrating all Resilience4j patterns applied to weather service:
 * - Circuit Breaker: Opens circuit after failure threshold
 * - Retry: Automatically retries failed calls
 * - Rate Limiter: Limits calls per time period
 * - Bulkhead: Isolates thread pools
 * - Time Limiter: Enforces maximum execution time
 * - Fallback methods: Provides graceful degradation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceDemoService {

    private final SimulatedRemoteService simulatedRemoteService;

    /**
     * Demonstrates Circuit Breaker pattern for weather API calls
     * Opens circuit after failure threshold is reached
     */
    @CircuitBreaker(name = "remoteService", fallbackMethod = "circuitBreakerFallback")
    public RemoteServiceResponseDto callWithCircuitBreaker(RemoteServiceRequestDto request) {
        log.info("Fetching weather for {} with Circuit Breaker protection", 
                request.getCity() != null ? request.getCity() : "unknown city");
        try {
            RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
            response.setResiliencePattern("Circuit Breaker");
            return response;
        } catch (IOException | TimeoutException | InterruptedException e) {
            // Re-throw as RuntimeException so Resilience4j Circuit Breaker can intercept it
            // The Circuit Breaker will catch this and call the fallback method
            throw new RuntimeException("Weather service call failed", e);
        }
    }

    /**
     * Fallback method for Circuit Breaker
     * Returns cached/default weather data when circuit is open
     */
    public RemoteServiceResponseDto circuitBreakerFallback(RemoteServiceRequestDto request, Exception ex) {
        String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        log.warn("Circuit Breaker fallback triggered for {}. Reason: {}", 
                request.getCity(), errorMessage);
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("Circuit Breaker");
        fallback.setDescription("Circuit Breaker fallback - Weather service unavailable. Showing cached weather data.");
        return fallback;
    }

    /**
     * Demonstrates Retry pattern for weather API calls
     * Automatically retries failed calls up to max attempts
     */
    @Retry(name = "remoteService", fallbackMethod = "retryFallback")
    public RemoteServiceResponseDto callWithRetry(RemoteServiceRequestDto request) 
            throws IOException, TimeoutException, InterruptedException {
        log.info("Fetching weather for {} with Retry protection", 
                request.getCity() != null ? request.getCity() : "unknown city");
        RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
        response.setResiliencePattern("Retry");
        return response;
    }

    /**
     * Fallback method for Retry
     * Returns default weather data after all retry attempts are exhausted
     */
    public RemoteServiceResponseDto retryFallback(RemoteServiceRequestDto request, Exception ex) {
        log.warn("Retry exhausted - All attempts failed for {}. Reason: {}", 
                request.getCity(), ex.getMessage());
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("Retry");
        fallback.setDescription("Retry exhausted - Weather service unavailable after all retry attempts. Showing cached weather data.");
        return fallback;
    }

    /**
     * Demonstrates Rate Limiter pattern for weather API calls
     * Limits number of calls per time period
     */
    @RateLimiter(name = "remoteService", fallbackMethod = "rateLimiterFallback")
    public RemoteServiceResponseDto callWithRateLimiter(RemoteServiceRequestDto request) 
            throws IOException, TimeoutException, InterruptedException {
        log.info("Fetching weather for {} with Rate Limiter protection", 
                request.getCity() != null ? request.getCity() : "unknown city");
        RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
        response.setResiliencePattern("Rate Limiter");
        return response;
    }

    /**
     * Fallback method for Rate Limiter
     * Returns default weather data when rate limit is exceeded
     */
    public RemoteServiceResponseDto rateLimiterFallback(RemoteServiceRequestDto request, Exception ex) {
        log.warn("Rate limit exceeded for weather API. City: {}. Reason: {}", 
                request.getCity(), ex.getMessage());
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("Rate Limiter");
        fallback.setDescription("Rate limit exceeded - Too many requests. Please try again later. Showing cached weather data.");
        return fallback;
    }

    /**
     * Demonstrates Bulkhead pattern for weather API calls
     * Isolates thread pools to prevent resource exhaustion
     */
    @Bulkhead(name = "remoteService", fallbackMethod = "bulkheadFallback")
    public RemoteServiceResponseDto callWithBulkhead(RemoteServiceRequestDto request) 
            throws IOException, TimeoutException, InterruptedException {
        log.info("Fetching weather for {} with Bulkhead protection", 
                request.getCity() != null ? request.getCity() : "unknown city");
        RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
        response.setResiliencePattern("Bulkhead");
        return response;
    }

    /**
     * Fallback method for Bulkhead
     * Returns default weather data when max concurrent calls are reached
     */
    public RemoteServiceResponseDto bulkheadFallback(RemoteServiceRequestDto request, Exception ex) {
        log.warn("Bulkhead full - Max concurrent calls reached for weather API. City: {}. Reason: {}", 
                request.getCity(), ex.getMessage());
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("Bulkhead");
        fallback.setDescription("Bulkhead full - Maximum concurrent calls reached. Please try again later. Showing cached weather data.");
        return fallback;
    }

    /**
     * Demonstrates Time Limiter pattern (async) for weather API calls
     * Enforces maximum execution time for async operations
     */
    @TimeLimiter(name = "remoteService", fallbackMethod = "timeLimiterFallback")
    public CompletableFuture<RemoteServiceResponseDto> callWithTimeLimiter(RemoteServiceRequestDto request) {
        log.info("Fetching weather for {} with Time Limiter protection (async)", 
                request.getCity() != null ? request.getCity() : "unknown city");
        return CompletableFuture.supplyAsync(() -> {
            try {
                RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
                response.setResiliencePattern("Time Limiter");
                return response;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Fallback method for Time Limiter
     * Returns default weather data when operation times out
     */
    public CompletableFuture<RemoteServiceResponseDto> timeLimiterFallback(RemoteServiceRequestDto request, Exception ex) {
        log.warn("Time Limiter exceeded for weather API. City: {}. Reason: {}", 
                request.getCity(), ex.getMessage());
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("Time Limiter");
        fallback.setDescription("Time Limiter exceeded - Operation timed out. Showing cached weather data.");
        return CompletableFuture.completedFuture(fallback);
    }

    /**
     * Demonstrates combination of Circuit Breaker + Retry for weather API calls
     */
    @CircuitBreaker(name = "remoteService", fallbackMethod = "combinedCircuitBreakerFallback")
    @Retry(name = "remoteService")
    public RemoteServiceResponseDto callWithCircuitBreakerAndRetry(RemoteServiceRequestDto request) 
            throws IOException, TimeoutException, InterruptedException {
        log.info("Fetching weather for {} with Circuit Breaker + Retry protection", 
                request.getCity() != null ? request.getCity() : "unknown city");
        RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
        response.setResiliencePattern("Circuit Breaker + Retry");
        return response;
    }

    /**
     * Fallback method for combined Circuit Breaker + Retry
     */
    public RemoteServiceResponseDto combinedCircuitBreakerFallback(RemoteServiceRequestDto request, Exception ex) {
        log.warn("Circuit Breaker + Retry exhausted for weather API. City: {}. Reason: {}", 
                request.getCity(), ex.getMessage());
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("Circuit Breaker + Retry");
        fallback.setDescription("Circuit Breaker + Retry exhausted - Weather service unavailable. Showing cached weather data.");
        return fallback;
    }

    /**
     * Demonstrates all Resilience4j patterns combined for weather API calls
     */
    @CircuitBreaker(name = "remoteService", fallbackMethod = "allPatternsFallback")
    @Retry(name = "remoteService")
    @RateLimiter(name = "remoteService")
    @Bulkhead(name = "remoteService")
    public RemoteServiceResponseDto callWithAllPatterns(RemoteServiceRequestDto request) 
            throws IOException, TimeoutException, InterruptedException {
        log.info("Fetching weather for {} with ALL Resilience4j patterns", 
                request.getCity() != null ? request.getCity() : "unknown city");
        RemoteServiceResponseDto response = simulatedRemoteService.callRemoteService(request);
        response.setResiliencePattern("All Patterns (Circuit Breaker + Retry + Rate Limiter + Bulkhead)");
        return response;
    }

    /**
     * Fallback method for all patterns combined
     */
    public RemoteServiceResponseDto allPatternsFallback(RemoteServiceRequestDto request, Exception ex) {
        log.warn("All Resilience4j patterns applied - Weather service unavailable. City: {}. Reason: {}", 
                request.getCity(), ex.getMessage());
        RemoteServiceResponseDto fallback = simulatedRemoteService.getDefaultWeatherData(
                request.getCity(), request.getCountry(), request.getUnits());
        fallback.setResiliencePattern("All Patterns");
        fallback.setDescription("All Resilience4j patterns applied - Weather service unavailable. Showing cached weather data.");
        return fallback;
    }
}
