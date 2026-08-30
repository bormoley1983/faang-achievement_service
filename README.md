# Achievement Service
Service responsible for managing user achievements, achievement progress tracking, and related business logic.

## Quick start

Prerequisites:
- Java 25+ (JDK)
- Docker (for container runs)
- [faang-infra services](https://github.com/bormoley1983/faang-infra) running locally or accessible

Run locally:
```sh
./gradlew bootRun
```

Run tests:
```sh
./gradlew test --info
```

Build and run in Docker:
```sh
./gradlew build
docker build -t achievement-service .
docker run -p 8085:8085 achievement-service
```

## Configuration

Main config: [src/main/resources/application.yaml](src/main/resources/application.yaml)

## External Integrations

Feign clients:
- [UserServiceClient](src/main/java/faang/school/achievement/client/UserServiceClient.java) — validates referenced users
- Feign configuration: [FeignConfig](src/main/java/faang/school/achievement/client/FeignConfig.java), [FeignUserInterceptor](src/main/java/faang/school/achievement/client/FeignUserInterceptor.java)

## Suggested Improvements

- Add API endpoint documentation with example requests for key operations
- Add docker-compose example for local development with PostgreSQL
- Move database credentials to environment variables in production; currently using defaults for local dev: [src/main/resources/application.yaml](src/main/resources/application.yaml)
- Document the integration points with external services via Feign clients
- Add achievement progression algorithms documentation
- Expand AssertJ, JUnit 5, and parameterized-test coverage for achievement rules.
- Document the Docker image and runtime health checks.

**Note:** Base code structure and architecture patterns are based on [FAANG School](https://github.com/faang-school) educational project.
