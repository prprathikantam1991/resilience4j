package com.pradeep.resilience4j.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Open-Meteo Forecast API response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastResponseDto {
    @JsonProperty("current_weather")
    private CurrentWeather currentWeather;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeather {
        private Double temperature; // Temperature in Celsius
        private Double windspeed; // Wind speed in km/h
        private Integer winddirection; // Wind direction in degrees
        private Integer weathercode; // WMO weather code
        private String time; // ISO 8601 timestamp
    }
}

