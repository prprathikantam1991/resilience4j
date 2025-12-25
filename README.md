# Resilience4j Weather Service

A Spring Boot 3 application demonstrating all major Resilience4j design patterns (Circuit Breaker, Retry, Rate Limiter, Bulkhead, Time Limiter) with Micrometer metrics, Prometheus monitoring, and Grafana dashboards. Built with **Spring Boot 3.5.3**, **Resilience4j 2.1.0**, **Micrometer**, **Prometheus**, and **Grafana**.

## Features

- **Circuit Breaker**: Prevents cascading failures by opening circuit after failure threshold
- **Retry**: Automatically retries failed calls with exponential backoff
- **Rate Limiter**: Limits number of calls per time period (token bucket algorithm)
- **Bulkhead**: Isolates thread pools to prevent resource exhaustion
- **Time Limiter**: Enforces maximum execution time for async operations
- **Fallback Methods**: Graceful degradation with default/cached data
- **Metrics & Monitoring**: Prometheus metrics export and Grafana dashboards
- **API Documentation**: Swagger/OpenAPI UI for interactive API testing

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (for monitoring stack)

### Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access Points

- **Application API**: http://localhost:8080/api/weather
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator Health**: http://localhost:8080/actuator/health
- **Actuator Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/weather` | Get weather data using all Resilience4j patterns |
| `POST` | `/api/weather/circuit-breaker` | Test Circuit Breaker pattern |
| `POST` | `/api/weather/retry` | Test Retry pattern |
| `POST` | `/api/weather/rate-limiter` | Test Rate Limiter pattern |
| `POST` | `/api/weather/bulkhead` | Test Bulkhead pattern |
| `POST` | `/api/weather/time-limiter` | Test Time Limiter pattern (async) |
| `POST` | `/api/weather/all-patterns` | Test all patterns combined |
| `GET` | `/api/weather/metrics` | Get custom Resilience4j metrics |

**Example Request**:

```json
POST /api/weather/circuit-breaker
{
  "city": "Boston",
  "state": "Massachusetts",
  "country": "US",
  "units": "C",
  "scenario": "success"
}
```

**Available Scenarios**: `success`, `failure`, `timeout`, `slow`, `random`

## Monitoring Setup

Start Prometheus and Grafana using Docker Compose:

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

The Grafana dashboard is auto-loaded with pre-configured panels for all Resilience4j metrics, JVM metrics, and HTTP metrics.

## Configuration

Key configuration files:

- **`src/main/resources/application.yaml`**: Resilience4j pattern configurations, Actuator settings, and Micrometer metrics export
- **`prometheus/prometheus.yml`**: Prometheus scrape configuration targeting Spring Boot Actuator
- **`docker-compose.monitoring.yml`**: Docker Compose configuration for Prometheus and Grafana services
- **`grafana/provisioning/`**: Grafana datasource and dashboard provisioning (auto-configuration)

## Project Structure

```
src/main/java/com/pradeep/resilience4j/
├── controller/          # REST API endpoints
├── service/            # Business logic with Resilience4j annotations
├── config/             # WebClient configuration for external APIs
└── dto/                # Request/Response DTOs

src/main/resources/
└── application.yaml    # Application and Resilience4j configuration

prometheus/
└── prometheus.yml      # Prometheus scrape configuration

grafana/
├── provisioning/       # Auto-configuration for datasources and dashboards
└── dashboards/        # Pre-built Grafana dashboard JSON
```
