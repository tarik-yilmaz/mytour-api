# AGENTS.md

Guidelines for implementing the MyTour Spring Boot API. Follow these rules when changing code in this module.

## Project Goals

- Build a Spring Boot backend for the Tour Planner semester project with a clear presentation, business, and data access layer.
- Support Angular over HTTP/JSON, PostgreSQL persistence via JPA/Hibernate, external OpenRouteService calls, logging, validation, authentication, import/export, full-text search, and computed tour attributes.
- Keep all user data private to its owner: tours and tour logs are never shared across users.
- Preserve externally configured values. Database credentials, API keys, CORS origins, filesystem paths, ports, JWT secrets, and upstream URLs belong in properties/environment variables, not hard-coded source.
- Keep the backend easy to demonstrate: local Docker Compose, health checks, Swagger/OpenAPI, and tests should keep working.

## Current Stack

- Java 25 and Spring Boot Maven project.
- Base package: `org.fhtw.mytourapi`.
- Servlet MVC stack with `spring-boot-starter-webmvc`.
- JPA/Hibernate with PostgreSQL.
- Jakarta Bean Validation through `spring-boot-starter-validation`.
- Springdoc OpenAPI UI.
- Logback via Spring Boot logging.
- Actuator health/info endpoints.

## Architecture

- Keep classes under the base package so Spring component scanning finds them.
- Use a layered package structure:
  - `controller` for inbound REST controllers only.
  - `service` for business use cases, computed attributes, transactions, orchestration, import/export, and OpenRouteService coordination.
  - `repository` for Spring Data repositories.
  - `model` or `domain` for JPA entities and domain value types.
  - `dto` for request/response records used by the API.
  - `mapper` for entity/DTO conversion.
  - `config` for configuration properties, CORS, security, OpenAPI, clients, and infrastructure beans.
  - `security` for authentication, JWT, current-user resolution, and authorization helpers.
  - `client` for outbound REST clients and their DTOs.
  - `exception` for domain exceptions and REST error handling.
- Controllers must not call repositories or outbound REST clients directly. Controllers call services; services call repositories and clients.
- Prefer constructor injection for every dependency. Avoid field injection.
- Use `@Service`, `@Repository`, `@Configuration`, and `@RestController` intentionally. Use `@Bean` methods for infrastructure objects configured from properties.
- Keep domain logic out of controllers. Popularity, child-friendliness, ownership checks, import/export rules, and search logic belong in services.

## REST API Rules

- Use resource-oriented endpoints under a consistent API prefix, for example `/api/auth`, `/api/tours`, and `/api/tours/{tourId}/logs`.
- Use HTTP methods conventionally:
  - `GET` for reads.
  - `POST` for create/import/actions that create state.
  - `PUT` or `PATCH` for updates.
  - `DELETE` for deletion.
- Use DTOs for all request and response bodies. Do not expose JPA entities directly from controllers.
- Return appropriate status codes:
  - `200 OK` for successful reads/updates.
  - `201 Created` for creates, preferably with a `Location` header.
  - `204 No Content` for successful deletes.
  - `400 Bad Request` for validation or malformed input.
  - `401 Unauthorized` when no valid authentication is present.
  - `403 Forbidden` when the user is authenticated but cannot access the resource.
  - `404 Not Found` when a resource does not exist or does not belong to the current user.
  - `409 Conflict` for duplicate or inconsistent state.
  - `502 Bad Gateway` or `503 Service Unavailable` for upstream OpenRouteService failures where appropriate.
- Centralize error responses with `@RestControllerAdvice`. Avoid leaking stack traces or persistence details.
- Add validation annotations such as `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`, `@Positive`, `@Email`, and nested `@Valid` on DTOs.
- Keep CORS centralized in configuration; do not scatter `@CrossOrigin` on controllers unless there is a specific reason.
- Keep the generated OpenAPI contract useful by using clear DTO names, route names, status codes, and summaries where needed.

## Persistence and Data Model

- Use JPA entities for persisted state and Spring Data repositories for access.
- Model ownership explicitly. Each `Tour` and `TourLog` must be linked to a `User`; all repository queries for user-owned data must include the user id or owner.
- Model the required data:
  - Tour: name, description, from, to, transport type, distance, estimated time, route information/map data, image/file reference if used, owner.
  - TourLog: date/time, comment, difficulty, total distance, total time, rating, tour, owner through tour or explicit relation.
  - User: credentials and identity data required for registration/login.
- Store images externally on the filesystem and persist only metadata/path references in the database.
- Avoid clear-text secrets. Passwords must be hashed with a Spring Security `PasswordEncoder`; API tokens/JWT secrets come from configuration.
- Prefer explicit relationships:
  - `Tour` has many `TourLog` entries.
  - Use cascading only where deletion semantics are intentional and tested.
  - Be careful with bidirectional relationships and JSON serialization; DTOs should avoid recursion.
- For schema evolution, prefer Flyway migrations and set Hibernate DDL generation to `none` once migrations are introduced. Do not rely on `ddl-auto=update` for final hand-in quality.
- Keep transactions in services using `@Transactional`; use `readOnly = true` for read-only methods.
- Disable or avoid Open Session in View, and design service methods to load everything the response needs.

## Search and Computed Values

- Full-text search must cover tour and tour-log data and also the computed values required by the assignment.
- Keep search behavior in a dedicated service/repository query path so it can be tested and documented with a sequence diagram.
- Computed values:
  - Popularity is derived from the number of logs for a tour.
  - Child-friendliness is derived from difficulty, total time, and distance values from logs.
- Make formulas deterministic and document-worthy. Keep them in one service or calculator class with focused unit tests.
- Decide whether computed values are calculated on read or stored/updated. If stored, keep update rules transactional and tested.

## OpenRouteService and External REST Calls

- Encapsulate OpenRouteService integration in `client` plus a business service. Controllers never call it directly.
- Externalize base URL, API key, profile mappings, connect timeout, and read timeout through configuration properties.
- Use explicit DTOs for upstream requests/responses and map them to the domain.
- Handle upstream failures deliberately:
  - Map 4xx responses to actionable domain exceptions.
  - Map 5xx/timeouts/network errors to upstream-unavailable exceptions.
  - Set timeouts on all outbound calls.
  - Retry only idempotent requests, and only with bounded backoff.
- Log upstream request identifiers, route profile, latency, and failure class without logging API keys.
- Test clients with stubs/mocks so tests do not depend on the real OpenRouteService.

## Security

- Use Spring Security for authentication and endpoint protection.
- REST APIs should be stateless. Prefer JWT bearer authentication for the Angular client.
- Permit only registration, login, health, and OpenAPI endpoints as public where appropriate. Protect tour and log endpoints.
- Implement a `SecurityFilterChain` rather than relying on Spring Boot's default generated login/basic-auth behavior.
- Store JWT secret, issuer, and expiration in configuration.
- Authorize by ownership in the service/repository layer, not only by route shape.
- Never return another user's resources. For user-owned resources, returning `404` is acceptable to avoid revealing existence.

## Logging and Observability

- Use SLF4J/Logback (`LoggerFactory` or Lombok `@Slf4j`) instead of `System.out`.
- Log important lifecycle and business events at `info`, expected validation/user errors at low/no logging, suspicious authorization failures at `warn`, and unexpected failures at `error`.
- Do not log passwords, JWTs, API keys, or full personal data.
- Keep actuator health/info available for local and Docker Compose checks.
- Use package-level logging configuration in `application.properties` when deeper debugging is needed.

## Testing

- The final project requires at least 20 meaningful JUnit tests. Add tests as features are implemented rather than saving them for the end.
- Prefer focused unit tests for:
  - service business rules,
  - computed popularity and child-friendliness,
  - validation edge cases,
  - authorization/ownership decisions,
  - import/export parsing and serialization,
  - mapping logic.
- Use MockMvc or Spring MVC tests for controller contracts, validation responses, and status codes.
- Use repository/integration tests for JPA queries, ownership filters, search behavior, and migrations. Prefer isolated test configuration or containers when practical.
- Stub external REST calls; tests should not call OpenRouteService.
- Keep `contextLoads` but do not count on it as meaningful feature coverage.
- Before handing work back, run the narrowest relevant Maven test command, usually `./mvnw test` from `mytour-api` if time permits.

## Configuration and Profiles

- Keep defaults developer-friendly, but put real local values in `.env` or environment variables.
- Keep `.env.example` documented and free of real secrets.
- Add properties with clear prefixes, for example `security.jwt.*`, `clients.openrouteservice.*`, `storage.images.*`.
- Use `@ConfigurationProperties` for grouped settings instead of many unrelated `@Value` fields.
- Keep database URL, username, password, CORS origins, server port, image base directory, and upstream API key configurable.

## Import, Export, and Files

- Pick one explicit import/export format and keep it documented. JSON is the natural default for this API.
- Validate imported data before saving it.
- Import must preserve ownership: imported tours/logs belong to the authenticated user performing the import.
- Do not trust file names or paths from clients. Normalize and constrain filesystem access to the configured image/base directory.
- Keep exported data free of password hashes, tokens, and unrelated user data.

## Code Style

- Prefer small, readable classes with one responsibility.
- Prefer Java records for immutable DTOs.
- Use Lombok where it improves clarity, but do not hide important constructors or domain behavior.
- Use `Optional` from repositories carefully; convert missing values to domain exceptions in services.
- Avoid returning `null` from service methods.
- Keep comments sparse and useful. Explain non-obvious formulas, security decisions, or integration behavior.
- Do not introduce broad refactors while implementing a feature unless the feature requires it.

## Implementation Checklist

When adding a backend feature, make sure it has:

1. DTOs with validation.
2. Controller endpoint with correct status codes.
3. Service method containing business logic and ownership checks.
4. Repository query scoped to the current user where applicable.
5. Entity/migration changes if persistence is needed.
6. OpenAPI-visible request/response types.
7. Logging for important failures or integrations.
8. Tests for success, validation failure, missing resource, and ownership/security behavior.

