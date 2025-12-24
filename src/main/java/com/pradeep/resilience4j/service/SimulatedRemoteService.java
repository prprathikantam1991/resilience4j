package com.pradeep.resilience4j.service;

import com.pradeep.resilience4j.dto.request.RemoteServiceRequestDto;
import com.pradeep.resilience4j.dto.response.ForecastResponseDto;
import com.pradeep.resilience4j.dto.response.GeocodingResponseDto;
import com.pradeep.resilience4j.dto.response.RemoteServiceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Simulates an external weather API service that can fail in various ways
 * for testing Resilience4j patterns.
 * This service mimics a real weather API like OpenWeatherMap or Weather.com
 */
@Service
@Slf4j
public class SimulatedRemoteService {

    private final WebClient geocodingWebClient;
    private final WebClient forecastWebClient;
    private final Random random = new Random();

    public SimulatedRemoteService(
            @Qualifier("geocodingWebClient") WebClient geocodingWebClient,
            @Qualifier("forecastWebClient") WebClient forecastWebClient) {
        this.geocodingWebClient = geocodingWebClient;
        this.forecastWebClient = forecastWebClient;
    }
    
    // Weather conditions for realistic data generation
    private static final String[] CONDITIONS = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Snowy", "Foggy", "Windy"};
    private static final String[] DESCRIPTIONS = {
        "Clear skies with bright sunshine",
        "Partly cloudy with occasional clouds",
        "Overcast with dense cloud cover",
        "Light to moderate rain expected",
        "Snowfall with cold temperatures",
        "Dense fog reducing visibility",
        "Strong winds with gusts"
    };

    /**
     * Simulates a weather API call with configurable behavior
     * 
     * @param request Weather request with city, country, and scenario
     * @return RemoteServiceResponseDto with weather data
     * @throws IOException if API failure is simulated
     * @throws TimeoutException if timeout is simulated
     * @throws InterruptedException if thread is interrupted
     */
    public RemoteServiceResponseDto callRemoteService(RemoteServiceRequestDto request) 
            throws IOException, TimeoutException, InterruptedException {
        
        String city = request.getCity() != null ? request.getCity() : "Unknown";
        String country = request.getCountry() != null ? request.getCountry() : "Unknown";
        String units = request.getUnits() != null ? request.getUnits() : "C";
        String scenario = request.getScenario() != null ? request.getScenario() : "success";
        Integer delayMs = request.getDelayMs() != null ? request.getDelayMs() : 0;
        Double failureProbability = request.getFailureProbability() != null 
                ? request.getFailureProbability() : 0.0;

        log.info("Simulating weather API call for city: {}, country: {}, scenario: {}, delay: {}ms, failure probability: {}", 
                city, country, scenario, delayMs, failureProbability);

        long startTime = System.currentTimeMillis();

        // Apply delay if specified
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }

        // Handle different scenarios
        switch (scenario.toLowerCase()) {
            case "failure":
                log.warn("Simulating weather API failure for city: {}", city);
                throw new IOException("Weather API service unavailable - External service returned 503 Service Unavailable");
            
            case "timeout":
                log.warn("Simulating weather API timeout for city: {}", city);
                throw new TimeoutException("Weather API request timed out - Service did not respond within timeout period");
            
            case "slow":
                // Simulate slow response (4 seconds)
                log.info("Simulating slow weather API response for city: {}", city);
                Thread.sleep(4000);
                break;
            
            case "random":
                // Random failure based on probability
                if (random.nextDouble() < failureProbability) {
                    int failureType = random.nextInt(3);
                    switch (failureType) {
                        case 0:
                            throw new IOException("Weather API returned error: Rate limit exceeded");
                        case 1:
                            throw new TimeoutException("Weather API request timed out");
                        case 2:
                            throw new RuntimeException("Weather API service error: Internal server error");
                    }
                }
                break;
            
            case "success":
            default:
                // Call real Open-Meteo APIs
                log.info("Calling real Open-Meteo weather APIs for city: {}", city);
                try {
                    RemoteServiceResponseDto weatherData = fetchRealWeatherData(request);
                    long executionTime = System.currentTimeMillis() - startTime;
                    weatherData.setExecutionTimeMs(executionTime);
                    weatherData.setTimestamp(LocalDateTime.now());
                    weatherData.setStatus("SUCCESS");
                    weatherData.setFromFallback(false);

                    log.info("Weather data retrieved successfully for {}: {}°{}, condition: {}", 
                            city, weatherData.getTemperature(), units, weatherData.getCondition());

                    return weatherData;
                } catch (Exception e) {
                    log.error("Failed to fetch real weather data, falling back to simulated data: {}", e.getMessage());
                    // Fall through to generateWeatherData for fallback
                }
                break;
        }

        // Generate realistic weather data (for non-success scenarios or fallback)
        RemoteServiceResponseDto weatherData = generateWeatherData(city, country, units);
        long executionTime = System.currentTimeMillis() - startTime;
        weatherData.setExecutionTimeMs(executionTime);
        weatherData.setTimestamp(LocalDateTime.now());
        weatherData.setStatus("SUCCESS");
        weatherData.setFromFallback(false);

        log.info("Weather data retrieved successfully for {}: {}°{}, condition: {}", 
                city, weatherData.getTemperature(), units, weatherData.getCondition());

        return weatherData;
    }

    /**
     * Generates realistic weather data for a given city
     */
    private RemoteServiceResponseDto generateWeatherData(String city, String country, String units) {
        // Generate realistic temperature based on season (simplified)
        double baseTemp = random.nextDouble() * 30 + 5; // 5-35°C
        if (units.equalsIgnoreCase("F")) {
            baseTemp = (baseTemp * 9/5) + 32; // Convert to Fahrenheit
        }
        
        // Generate realistic humidity (30-90%)
        double humidity = random.nextDouble() * 60 + 30;
        
        // Generate realistic pressure (980-1020 hPa)
        double pressure = random.nextDouble() * 40 + 980;
        
        // Generate realistic wind speed (0-30 km/h)
        double windSpeed = random.nextDouble() * 30;
        
        // Select random condition
        int conditionIndex = random.nextInt(CONDITIONS.length);
        String condition = CONDITIONS[conditionIndex];
        String description = DESCRIPTIONS[conditionIndex];
        
        // Adjust temperature based on condition
        if (condition.equals("Snowy")) {
            baseTemp = units.equalsIgnoreCase("F") ? random.nextDouble() * 20 + 20 : random.nextDouble() * 10 - 5;
        } else if (condition.equals("Rainy")) {
            baseTemp -= 5;
        }

        return RemoteServiceResponseDto.builder()
                .city(city)
                .country(country)
                .temperature(Math.round(baseTemp * 10.0) / 10.0) // Round to 1 decimal
                .humidity(Math.round(humidity * 10.0) / 10.0)
                .pressure(Math.round(pressure * 10.0) / 10.0)
                .condition(condition)
                .windSpeed(Math.round(windSpeed * 10.0) / 10.0)
                .units(units)
                .description(description)
                .build();
    }

    /**
     * Fetches real weather data from Open-Meteo APIs
     * 
     * @param request Weather request with city, state, country
     * @return RemoteServiceResponseDto with real weather data
     * @throws IOException if geocoding fails or no results found
     * @throws RuntimeException if API calls fail
     */
    private RemoteServiceResponseDto fetchRealWeatherData(RemoteServiceRequestDto request) 
            throws IOException {
        String city = request.getCity() != null ? request.getCity() : "Unknown";
        String state = request.getState();
        String country = request.getCountry() != null ? request.getCountry() : "Unknown";
        String units = request.getUnits() != null ? request.getUnits() : "C";

        log.info("Fetching geocoding data for city: {}, state: {}, country: {}", city, state, country);

        // Step 1: Call geocoding API to get latitude and longitude
        GeocodingResponseDto geocodingResponse = geocodingWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v1/search")
                            .queryParam("name", city)
                            .queryParam("count", 1);
                    if (state != null && !state.isEmpty()) {
                        uriBuilder.queryParam("state", state);
                    }
                    if (country != null && !country.isEmpty()) {
                        uriBuilder.queryParam("country", country);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(GeocodingResponseDto.class)
                .block();

        if (geocodingResponse == null || geocodingResponse.getResults() == null 
                || geocodingResponse.getResults().isEmpty()) {
            throw new IOException("City not found: " + city + ", " + country);
        }

        GeocodingResponseDto.GeocodingResult location = geocodingResponse.getResults().get(0);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String actualCity = location.getName();
        String actualCountry = location.getCountry() != null ? location.getCountry() : country;

        log.info("Found location: {} ({}, {}) at coordinates: {}, {}", 
                actualCity, actualCountry, location.getState(), latitude, longitude);

        // Step 2: Call forecast API with latitude and longitude
        log.info("Fetching weather forecast for coordinates: {}, {}", latitude, longitude);

        ForecastResponseDto forecastResponse = forecastWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current_weather", true)
                        .build())
                .retrieve()
                .bodyToMono(ForecastResponseDto.class)
                .block();

        if (forecastResponse == null || forecastResponse.getCurrentWeather() == null) {
            throw new IOException("Failed to retrieve weather forecast data");
        }

        ForecastResponseDto.CurrentWeather currentWeather = forecastResponse.getCurrentWeather();

        // Step 3: Map forecast response to our DTO
        double temperature = currentWeather.getTemperature();
        if (units.equalsIgnoreCase("F")) {
            // Convert Celsius to Fahrenheit
            temperature = (temperature * 9.0 / 5.0) + 32.0;
        }

        String condition = mapWeatherCodeToCondition(currentWeather.getWeathercode());
        String description = getWeatherDescription(currentWeather.getWeathercode());

        return RemoteServiceResponseDto.builder()
                .city(actualCity)
                .country(actualCountry)
                .temperature(Math.round(temperature * 10.0) / 10.0) // Round to 1 decimal
                .humidity(null) // Not available in current_weather endpoint
                .pressure(null) // Not available in current_weather endpoint
                .condition(condition)
                .windSpeed(Math.round(currentWeather.getWindspeed() * 10.0) / 10.0)
                .units(units)
                .description(description)
                .build();
    }

    /**
     * Maps WMO weather code to human-readable condition
     * Based on WMO Weather interpretation codes (WW)
     */
    private String mapWeatherCodeToCondition(Integer weathercode) {
        if (weathercode == null) {
            return "Unknown";
        }

        // WMO Weather interpretation codes
        if (weathercode == 0) {
            return "Clear Sky";
        } else if (weathercode == 1 || weathercode == 2 || weathercode == 3) {
            return "Partly Cloudy";
        } else if (weathercode >= 4 && weathercode <= 9) {
            return "Cloudy";
        } else if (weathercode >= 10 && weathercode <= 12) {
            return "Foggy";
        } else if (weathercode >= 13 && weathercode <= 19) {
            return "Windy";
        } else if (weathercode >= 20 && weathercode <= 29) {
            return "Rainy";
        } else if (weathercode >= 30 && weathercode <= 39) {
            return "Dust/Sand";
        } else if (weathercode >= 40 && weathercode <= 49) {
            return "Foggy";
        } else if (weathercode >= 50 && weathercode <= 59) {
            return "Rainy";
        } else if (weathercode >= 60 && weathercode <= 69) {
            return "Rainy";
        } else if (weathercode >= 70 && weathercode <= 79) {
            return "Snowy";
        } else if (weathercode >= 80 && weathercode <= 82) {
            return "Rainy";
        } else if (weathercode >= 85 && weathercode <= 86) {
            return "Snowy";
        } else if (weathercode >= 90 && weathercode <= 99) {
            return "Thunderstorm";
        } else {
            return "Unknown";
        }
    }

    /**
     * Gets detailed weather description based on weather code
     */
    private String getWeatherDescription(Integer weathercode) {
        if (weathercode == null) {
            return "Weather data unavailable";
        }

        // Simplified descriptions for common codes
        if (weathercode == 0) {
            return "Clear skies with bright sunshine";
        } else if (weathercode >= 1 && weathercode <= 3) {
            return "Partly cloudy with occasional clouds";
        } else if (weathercode >= 4 && weathercode <= 9) {
            return "Overcast with dense cloud cover";
        } else if (weathercode >= 10 && weathercode <= 12 || (weathercode >= 40 && weathercode <= 49)) {
            return "Dense fog reducing visibility";
        } else if (weathercode >= 50 && weathercode <= 69 || (weathercode >= 80 && weathercode <= 82)) {
            return "Light to moderate rain expected";
        } else if (weathercode >= 70 && weathercode <= 79 || (weathercode >= 85 && weathercode <= 86)) {
            return "Snowfall with cold temperatures";
        } else if (weathercode >= 90 && weathercode <= 99) {
            return "Thunderstorms with possible heavy rain";
        } else {
            return "Weather conditions variable";
        }
    }

    /**
     * Generates default/cached weather data for fallback scenarios
     */
    public RemoteServiceResponseDto getDefaultWeatherData(String city, String country, String units) {
        log.info("Generating default weather data for fallback - city: {}, country: {}", city, country);
        
        RemoteServiceResponseDto defaultData = generateWeatherData(
            city != null ? city : "Unknown", 
            country != null ? country : "Unknown", 
            units != null ? units : "C"
        );
        defaultData.setDescription("Last known weather data (cached)");
        defaultData.setStatus("FALLBACK");
        defaultData.setFromFallback(true);
        defaultData.setTimestamp(LocalDateTime.now());
        defaultData.setExecutionTimeMs(0L);
        
        return defaultData;
    }

}
