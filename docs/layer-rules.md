# Backend Layer Rules

Stand: 2026-07-04

## Ziel

Dieser Check dokumentiert die aktuelle Backend-Schichtung und stellt sicher,
dass produktiver Code keine hoeheren oder uebersprungenen Layer direkt
importiert.

## Gepruefte Paketkanten

Die aktuelle Projektstruktur folgt diesen Layern:

```text
controller -> service -> repository -> domain
controller -> dto
service    -> client/config/dto/exception
client     -> config/dto/exception
exception  -> dto
repository -> domain
mapper     -> domain/dto
```

`dto`, `domain` und `repository` bleiben dadurch passiv bzw. nach unten
gerichtet. Controller duerfen keine Repositories, Domains oder Clients direkt
verwenden. Clients duerfen keine Services kennen.

## Gefundene und behobene Abweichung

Beim Audit wurde ein Rueckwaertsimport gefunden:

```text
client.OpenRouteServiceDirectionsClient -> service.CalculatedRoute
client.RouteDirectionsClient            -> service.CalculatedRoute
```

Das wurde aufgeloest, indem der Client-Layer ein eigenes Ergebnis-Record
bekommen hat:

```text
client.RouteDirectionsResult
```

`RouteCalculationService` mappt dieses Client-Ergebnis jetzt auf sein
Service-Ergebnis:

```text
RouteDirectionsClient.fetchRoute(...)
  -> RouteDirectionsResult
  -> RouteCalculationService
  -> CalculatedRoute
```

Damit kennt der Client den Service-Layer nicht mehr.

## Automatischer Test

Der Test
`src/test/java/org/fhtw/mytourapi/architecture/LayerDependencyRulesTest.java`
scannt alle produktiven Java-Dateien unter `src/main/java/org/fhtw/mytourapi`
nach internen Imports und gleicht sie mit einer erlaubten Paketmatrix ab.

Der Test schuetzt insbesondere gegen:

- Controller, die Repositories, Domains oder Clients direkt importieren.
- Clients, die Services importieren.
- Repositories, die andere Schichten als Domain kennen.
- Domain-Klassen, die API-, Service- oder Repository-Klassen kennen.

Wenn spaeter neue Layer wie `mapper` oder `security` befuellt werden, koennen die
erlaubten Kanten im Test bewusst erweitert werden.
