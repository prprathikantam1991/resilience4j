package com.pradeep.resilience4j.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Open-Meteo Geocoding API response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResponseDto {
    private List<GeocodingResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodingResult {
        private String name;
        private Double latitude;
        private Double longitude;
        private String state;
        private String country;
    }
}

