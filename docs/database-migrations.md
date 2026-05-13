# Database Migrations

The backend uses Flyway to create and evolve the PostgreSQL schema. Hibernate is configured with `spring.jpa.hibernate.ddl-auto=validate`, so Flyway owns schema changes and Hibernate checks that the database still matches the JPA entities at startup.

## Migration Location

Put migration files in:

```text
src/main/resources/db/migration
```

The initial schema migration is:

```text
V1__init_schema.sql
```

## Adding A Migration

1. Create a new SQL file with the next version number.

   ```text
   V2__describe_the_change.sql
   ```

2. Write only the schema or data changes needed for that step.

   ```sql
   ALTER TABLE tours
   ADD COLUMN search_document TSVECTOR;

   CREATE INDEX idx_tours_search_document
   ON tours USING GIN (search_document);
   ```

3. Run the backend or tests so Flyway applies the migration and Hibernate validates the result.

   ```powershell
   cd mytour-api
   docker compose up --build
   ```

   Or, when running Maven locally, make sure the database credentials are available as `DB_USER` and `DB_PASSWORD`:

   ```powershell
   cd mytour-api
   $env:DB_USER = "postgres"
   $env:DB_PASSWORD = "your_secure_password_here"
   .\mvnw.cmd test
   ```

## Naming Rules

- Use the next unused version number: `V2`, `V3`, `V4`, and so on.
- Use two underscores between the version and description: `V2__add_search_index.sql`.
- Use descriptive names, preferably lowercase words separated by underscores.
- Never edit a migration that has already run on a shared or deployed database.
- If a migration is wrong after it has been applied, add a new migration that fixes it.

## Entity Change Workflow

When changing a JPA entity:

1. Update the entity class.
2. Add a matching Flyway migration.
3. Run the backend/tests.
4. Fix the migration or entity until both Flyway migration and Hibernate validation pass.

Example:

```text
TourEntity gains a new field
-> add V2__add_tour_field.sql
-> run backend/tests
-> Hibernate validates the new column exists and matches the mapping
```

## Checking Applied Migrations

Flyway records applied migrations in the `flyway_schema_history` table.

With Docker Compose running:

```powershell
cd mytour-api
docker compose exec db psql -U postgres -d mytourdb -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"
```

## Resetting The Local Database

The current Docker Compose setup does not define a persistent PostgreSQL volume. To recreate the local database from migrations:

```powershell
cd mytour-api
docker compose down
docker compose up --build
```

If a persistent volume is added later, reset with:

```powershell
docker compose down -v
docker compose up --build
```

