# SQL Injection Resistance Check

Stand: 2026-07-07

## Ziel

Dieser Check dokumentiert, dass die aktuelle Backend-Codebasis keine
benutzerkontrollierten Werte in handgebaute SQL-Strings einfuegt.

## Ergebnis des Code-Scans

Der Backend-Quellcode wurde nach manuellen SQL-/Query-APIs durchsucht:

```text
@Query
nativeQuery
createQuery
createNativeQuery
EntityManager
JdbcTemplate
NamedParameterJdbcTemplate
Statement
PreparedStatement
```

Im produktiven Java-Code gibt es keine Treffer fuer diese APIs. Der einzige
relevante Treffer beim breiteren Scan ist ein `StringBuilder` im
`TourSearchIndex`; dieser baut ein In-Memory-Suchdokument und keinen SQL-String.

## Aktuelle Datenzugriffsschicht

Die vorhandenen Repository-Interfaces verwenden Spring Data JPA mit abgeleiteten
Query-Methoden:

- `TourRepository`
- `TourLogRepository`
- `TourRouteRepository`
- `TourLogWeatherRepository`
- `UserRepository`

Beispiele:

```java
Optional<TourEntity> findByIdAndUser_Id(Long id, Long userId);
List<TourLogEntity> findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc(Long tourId, Long userId);
Optional<UserEntity> findByUsernameNormalized(String usernameNormalized);
```

Spring Data JPA erzeugt daraus parametrisierte Queries. Eingabewerte werden als
Parameter gebunden und nicht per String-Konkatenation in SQL eingefuegt.

## Aktuelle Suchstrecke

Die aktive Tour-Suche laeuft ueber den persistenten Tour-Service und einen
In-Memory-Suchindex fuer die aktuell geladenen Tour-/Log-Dokumente:

```text
TourController.searchTours(...)
  -> TourService.searchTours(...)
  -> TourSearchIndex.replaceLogs(...)
  -> TourSearchIndex.matches(...)
```

Der Suchindex normalisiert Benutzereingaben zu Suchbegriffen und vergleicht sie
mit einem Set von Dokument-Terms. Dabei wird kein SQL aus Benutzereingaben
erzeugt oder ausgefuehrt. Die Daten, die in den Suchindex eingehen, werden
vorher ueber Spring-Data-JPA-Repository-Methoden mit Parameterbindung und
Benutzer-Ownership-Filter geladen.

Als Regression-Test ist unter anderem vorhanden:

```text
TourSearchIndexTest
```

Die Suchtests stellen sicher, dass Suchtexte als normale Begriffe behandelt
werden, dass mehrere Begriffe per AND-Logik wirken und dass Log-/Wetter-/
Computed-Attribute nur ueber den Suchindex, nicht ueber dynamisches SQL,
ausgewertet werden.

## Regel fuer spaetere Persistenz-Tasks

Neue Abfragen sollen weiterhin ueber Spring Data JPA Derived Queries, Criteria
API oder explizit parametrisierte Queries laufen. Nicht erlaubt sind
String-Konkatenation mit Request-Werten, dynamische SQL-Fragmente aus
Benutzereingaben oder direkte JDBC-Statements ohne Parameterbindung.
