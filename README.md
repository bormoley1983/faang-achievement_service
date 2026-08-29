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
Test config: [src/test/resources/application-test.yaml](src/test/resources/application-test.yaml)

## External Integrations

Feign clients:
- [ProjectServiceClient](src/main/java/faang/school/achievement/client/ProjectServiceClient.java) — integration with project service
- Feign configuration: [FeignConfig](src/main/java/faang/school/achievement/client/FeignConfig.java), [FeignUserInterceptor](src/main/java/faang/school/achievement/client/FeignUserInterceptor.java)

## Suggested Improvements

- Add API endpoint documentation with example requests for key operations
- Document Redis pub/sub channels usage (`achievement_channel`, `follower_channel`) and their event flows
- Add docker-compose example for local development with PostgreSQL and Redis
- Move database credentials to environment variables in production; currently using defaults for local dev: [src/main/resources/application.yaml](src/main/resources/application.yaml)
- Document the integration points with external services via Feign clients
- Add achievement progression algorithms documentation
- Consider adding CI/CD workflow configuration (GitHub Actions or similar)
* AssertJ
* JUnit5
* Parameterized tests
* Dockerfile updates?
* Redis connectivity

**Note:** Base code structure and architecture patterns are based on [FAANG School](https://github.com/faang-school) educational project.