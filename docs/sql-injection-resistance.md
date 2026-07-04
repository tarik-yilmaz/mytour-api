# SQL Injection Resistance Check

Stand: 2026-07-04

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
`IntermediateTourSearchIndex`; dieser baut ein In-Memory-Suchdokument und keinen
SQL-String.

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

Die aktive Tour-Suche laeuft noch ueber den Intermediate-Service und ist
In-Memory:

```text
TourController.searchTours(...)
  -> IntermediateTourService.searchTours(...)
  -> IntermediateTourSearchIndex.matches(...)
```

Der Suchindex normalisiert Benutzereingaben zu Suchbegriffen und vergleicht sie
mit einem Set von Dokument-Terms. Dabei wird kein SQL erzeugt oder ausgefuehrt.

Als Regression-Test wurde ergaenzt:

```text
IntermediateTourServiceSearchTest.searchTreatsSqlInjectionLikeInputAsPlainText
```

Der Test stellt sicher, dass typische SQL-Injection-aehnliche Suchtexte wie
`anything' OR '1'='1` nicht als Steuerlogik wirken und nicht alle Touren
zurueckgeben.

## Regel fuer spaetere Persistenz-Tasks

Wenn die noch offenen DAL-Tasks fuer echte Tour-/TourLog-Persistenz umgesetzt
werden, sollen neue Abfragen weiterhin ueber Spring Data JPA Derived Queries,
Criteria API oder explizit parametrisierte Queries laufen. Nicht erlaubt sind
String-Konkatenation mit Request-Werten, dynamische SQL-Fragmente aus
Benutzereingaben oder direkte JDBC-Statements ohne Parameterbindung.
