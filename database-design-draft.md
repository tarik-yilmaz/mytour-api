# Database Design Draft

This draft is based on `semester-project.md` and the current project decisions for an Angular frontend, Spring Boot backend, JPA/Hibernate persistence, and PostgreSQL database.

## Design Goals

- Keep the schema small, normalized, and easy to map with Spring Data JPA.
- Use numeric database IDs, with security enforced through backend ownership checks.
- Store images on the filesystem and only keep metadata/path fields in PostgreSQL.
- Store route geometry as GeoJSON in PostgreSQL `jsonb`.
- Store actual timestamps in UTC and use a tour timezone for display.
- Support PostgreSQL full-text search over tours, tour logs, and computed labels.
- Support import/export including route geometry and weather snapshots.

## Tables

### `app_users`

Stores local application users for username/password registration and JWT login.

```text
app_users
- id PK
- username
- username_normalized UNIQUE
- password_hash
- created_at
- updated_at
- version
```

Notes:

- `username_normalized` stores a normalized value such as lowercase/trimmed username.
- This prevents `Ada`, `ada`, and ` ADA ` from registering as separate users.
- No email address, email verification, or email sending is required.
- `password_hash` stores a BCrypt hash, never a plain password.

### `tours`

Stores the main planned tour data and computed tour attributes.

```text
tours
- id PK
- user_id FK -> app_users.id
- name
- description
- start_location
- end_location
- transport_type
- timezone_id
- planned_distance_m
- estimated_duration_s
- cover_image_path
- cover_image_original_filename
- cover_image_content_type
- cover_image_size_bytes
- log_count
- popularity_score
- popularity_category
- popularity_label
- child_friendliness_score
- child_friendliness_category
- child_friendliness_label
- created_at
- updated_at
- version
```

Notes:

- Use `start_location` and `end_location` instead of SQL-unfriendly names like `from` and `to`.
- Store distance in meters and duration in seconds.
- `transport_type` values are `BIKE`, `HIKE`, `RUNNING`, and `VACATION`.
- `timezone_id` stores an IANA timezone such as `Europe/Vienna`.
- One cover image per tour is enough. The actual image file is stored externally on the filesystem.
- Computed attributes are stored on `tours` for fast list display, sorting, filtering, and full-text search.

### `tour_routes`

Stores route data returned by OpenRouteService.

```text
tour_routes
- tour_id PK/FK -> tours.id
- route_source
- route_profile
- start_lat
- start_lon
- end_lat
- end_lon
- midpoint_lat
- midpoint_lon
- route_geometry JSONB
- route_fetched_at
```

Notes:

- This table is separated from `tours` because route geometry is technical external API data.
- `route_geometry` stores GeoJSON as PostgreSQL `jsonb`.
- The JPA entity keeps this field as a Jackson `JsonNode` for PostgreSQL JSONB persistence, while the API DTO exposes it as a JSON-compatible `Map<String, Object>` so Spring Boot's Jackson 3 response serialization returns the actual GeoJSON tree.
- Leaflet can consume the GeoJSON data naturally in the Angular UI.
- `midpoint_lat` and `midpoint_lon` are used as the default weather lookup coordinate.

### `tour_logs`

Stores accomplished tour statistics.

```text
tour_logs
- id PK
- tour_id FK -> tours.id
- performed_at
- comment
- difficulty
- total_distance_m
- total_time_s
- rating
- created_at
- updated_at
- version
```

Notes:

- `performed_at` is stored as a UTC timestamp. In Java, model it as `Instant`.
- The frontend displays the time using the tour's `timezone_id`.
- `difficulty` is numeric from `1` to `5`.
- `rating` is numeric from `1` to `5`.
- The frontend can map numeric values to user-friendly labels.

### `tour_log_weather`

Stores the automatic weather snapshot for a tour log.

```text
tour_log_weather
- tour_log_id PK/FK -> tour_logs.id
- provider
- provider_dataset
- lookup_lat
- lookup_lon
- weather_observed_at
- temperature_c
- relative_humidity_percent
- precipitation_mm
- weather_code
- weather_description
- wind_speed_kmh
- fetched_at
```

Notes:

- This is an optional one-to-one table.
- A tour log can exist even if weather fetching fails.
- Weather is fetched from Open-Meteo using the midpoint between the tour start and end coordinates.
- For older logs, use Open-Meteo historical hourly weather.
- For very recent logs, use forecast/current endpoints as fallback if historical data is not available yet.
- Store only the selected snapshot values needed by the application, not the full external API response.
- Treat weather snapshots as generated immutable data. If a log's performed time or route coordinates change, refetch and replace the snapshot.

## Relationships

```text
app_users 1 -- * tours
tours 1 -- 0..1 tour_routes
tours 1 -- * tour_logs
tour_logs 1 -- 0..1 tour_log_weather
```

Deleting a tour should physically delete its route, logs, and weather rows through cascading foreign keys.

## Computed Attributes

Popularity is derived from the number of logs.

Example:

```text
0 logs      -> NEW
1-2 logs    -> RARELY_USED
3-5 logs    -> POPULAR
6+ logs     -> VERY_POPULAR
```

Child-friendliness is derived from recorded difficulty values, total times, and distances.

Example searchable labels:

```text
FAMILY_FRIENDLY              -> "family friendly"
MODERATE_FAMILY_SUITABILITY  -> "moderate family suitability"
CHALLENGING_ROUTE            -> "challenging route"
ADULT_ORIENTED               -> "adult oriented"
```

The design stores both numeric scores and labels:

```text
popularity_score
popularity_category
popularity_label
child_friendliness_score
child_friendliness_category
child_friendliness_label
```

Numeric values are useful for calculations and sorting. Labels are useful for display and full-text search.

Avoid searchable negated labels like `not child friendly`, because a search for `child friendly` could also match the negative phrase.

## Full-Text Search

Use PostgreSQL full-text search with GIN indexes.

Tour search should include:

```text
name
description
start_location
end_location
transport_type
popularity_label
child_friendliness_label
```

Tour log search should include:

```text
comment
difficulty/rating text if useful
weather_description if useful
```

For exact category matching, use structured filters in addition to full-text search:

```text
childFriendliness=FAMILY_FRIENDLY
popularity=POPULAR
transportType=BIKE
ratingMin=4
```

This avoids relying on fuzzy keyword matching for precise business categories.

## Security Rules

Numeric IDs are acceptable if every backend query is scoped by the authenticated user.

Examples:

```text
find tour by tour_id AND user_id
find log by log_id AND tour.user_id
update tour by tour_id AND user_id
delete log by log_id AND tour.user_id
```

Never update, delete, or return a tour/log by its numeric ID alone.

## Spring Boot/JPA Mapping

Recommended entity relationships:

```text
UserEntity 1 -- * TourEntity
TourEntity 1 -- 0..1 TourRouteEntity
TourEntity 1 -- * TourLogEntity
TourLogEntity 1 -- 0..1 TourLogWeatherEntity
```

Recommendations:

- Use lazy loading by default.
- Use DTOs for API input/output instead of exposing entities directly.
- Use `@Version` for optimistic locking.
- Use service-layer methods for ownership checks and computed attribute recalculation.
- Use repository methods or native queries for PostgreSQL full-text search.
