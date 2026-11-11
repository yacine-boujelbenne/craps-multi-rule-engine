# Phase 0.2: Spring Boot skeleton
**Agent:** backend (Codex) | **Branch:** feat/backend-skeleton
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Generate a Maven Spring Boot 3 project in `backend/` (Java 21, group `com.exportdocs`, artifact `api`) with dependencies: web, validation, data-jpa, security, actuator, postgresql, flyway-core, flyway-database-postgresql, lombok, springdoc-openapi-starter-webmvc-ui, mapstruct, testcontainers (postgresql, junit-jupiter). Use the Maven wrapper. If `start.spring.io` is unreachable, create the `pom.xml` by hand.
- [ ] Create package skeleton `domain`, `application`, `infrastructure`, `api` with a `package-info.java` in each stating its responsibility.
- [ ] Write the failing test `HealthEndpointIT` (Testcontainers Postgres, `GET /actuator/health` returns 200 and `status` = `UP`). Run it, see it fail or error, then configure `application.yml` (datasource from env `SPRING_DATASOURCE_URL`, `POSTGRES_*` defaults matching `docker-compose.yml`, Flyway enabled, JPA `ddl-auto: validate`) until it passes.
- [ ] Add a global `@RestControllerAdvice` returning RFC 7807 `ProblemDetail` for validation and not-found errors, with a unit test for each.
- [ ] Run `./mvnw verify`, then commit `feat: backend skeleton`.
