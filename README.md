# MyTour API

A Spring Boot backend application for the MyTour project.

## Running Locally with Docker Compose

The easiest way to run the project locally is by using Docker Compose. The compose setup starts PostgreSQL, the Spring Boot API, and the Angular frontend.

### Prerequisites
* [Docker](https://docs.docker.com/get-docker/) installed and running.
* [Docker Compose](https://docs.docker.com/compose/install/) (usually included with Docker Desktop).

### Setup

1. **Environment Variables:** 
   Copy the example environment file to create your local configuration:
   ```bash
   cp .env.example .env
   ```
   *(Note: On Windows Command Prompt, use `copy .env.example .env`)*
   
   If needed, open `.env` and adjust the ports or allowed origins:
   * `APP_PORT` exposes the backend API, default `8080` in `.env.example`.
   * `UI_PORT` exposes the Angular frontend, default `4200`.
   * `CORS_ALLOWED_ORIGINS` should include the frontend origin, for example `http://localhost:4200`.
   * `ORS_API_KEY` enables real OpenRouteService Directions responses. Leave it empty for the local fallback route.
   * `OPEN_METEO_*` configures automatic weather snapshots. The service uses local fallback data if the upstream is unavailable.
   * `IMAGE_STORAGE_BASE_DIRECTORY` controls where uploaded cover-image files are stored. PostgreSQL stores only metadata/path references.

2. **Start the Application:**
   Run the following command from the `mytour-api` directory:
   ```bash
   docker compose up --build
   ```
   * The `--build` flag ensures that the backend and frontend images are built with your latest code changes.
   * To run it in the background, you can add the `-d` flag: `docker compose up -d --build`.

3. **Access the Application:**
   Once the database, API, and frontend containers are running and healthy, the app will be accessible at:
   * **Frontend:** `http://localhost:4200`
   * **Base URL:** `http://localhost:8080`
   * **Health Check:** `http://localhost:8080/actuator/health`
   * **Swagger UI / OpenAPI docs:** `http://localhost:8080/swagger-ui.html`

The frontend service waits for the backend health check before starting.

### Running Locally without Docker

To run only the backend against a configured PostgreSQL database:

```powershell
.\mvnw.cmd spring-boot:run
```

When started directly, Spring Boot listens on the configured `PORT` value and usually defaults to `http://localhost:8080`.

## Database Migrations

The backend uses Flyway migrations for PostgreSQL schema creation and Hibernate validates the schema at startup with `spring.jpa.hibernate.ddl-auto=validate`.

Migration files live in:

```text
src/main/resources/db/migration
```

To add a schema change, create the next versioned SQL file, for example:

```text
V2__add_tour_search_columns.sql
```

Then run the backend or tests so Flyway applies the migration before Hibernate validates the JPA mappings:

```bash
docker compose up --build
```

Do not edit migrations that have already run on a shared database. Add a new `V3__...sql`, `V4__...sql`, etc. instead.

See the full guide: [Database Migrations](docs/database-migrations.md).

The current database/domain diagram is [database-class-diagram.puml](database-class-diagram.puml). It was checked against `V1__init_schema.sql` on 2026-07-07; columns and relationships match the DB init migration. Detailed constraints, indexes, defaults, and cascade rules are kept in the Flyway SQL file.

## API Contract and Route Geometry

The API contract is generated from backend DTOs through Springdoc OpenAPI and consumed by the Angular frontend through `ng-openapi-gen`.

After backend DTO/controller changes, start the backend and run this from `mytour-ui`:

```powershell
$env:API_URL='http://localhost:8080'; npm run sync-api
```

`TourRouteEntity.routeGeometry` is stored as PostgreSQL `jsonb` through a Jackson `JsonNode`. The public `TourRouteDto.routeGeometry` is exposed as a JSON-compatible object map so API responses contain the actual OpenRouteService GeoJSON `FeatureCollection` with `features`, `geometry`, and `coordinates`.

## Tests

Run backend tests:

```powershell
.\mvnw.cmd test
```

Latest verification: 124 backend tests passed on 2026-07-07.

### Stopping the Application

To stop the containers and remove them, press `Ctrl+C` in your terminal (if running in the foreground), or run:
```bash
docker compose down
```
*(Add `-v` if you also want to delete the local database volume and start fresh next time).*
