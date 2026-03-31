# Projektgedaechtnis — Railway Order Management

> Dieses Dokument wird nach groesseren funktionalen Aenderungen mit dem aktuellen Ist-Stand nachgezogen.

## Letzte Aktualisierung

**2026-03-31** — Dokumentation konsolidiert fuer alle Auftragspositionstypen (`LEISTUNG`, `FAHRPLAN`), Fahrplanbuilder, Fahrplanarchiv und aktuelle UI-Darstellung

## Projektstatus

**Order CRUD + Auftragspositionen (`LEISTUNG` / `FAHRPLAN`) + Fahrplanbuilder + Bestellkalender + RINF Import + Schlagwort-Katalog**

## Module / Bounded Contexts

| Modul | Status | Entities | Views / Komponenten | Bemerkung |
|---|---|---|---|---|
| **Order** | CRUD aktiv | `Order`, `OrderPosition`, `ResourceNeed`, `PurchasePosition` + Status-/Typ-Enums | `OrderListView`, `OrderDetailView`, `OrderFormPanel`, `OrderPositionPanel`, `ServicePositionDialog` | Auftragsuebersicht mit Status-Chips, Detailansicht mit angereicherten Positionszeilen, Leistungspositionen als Dialog |
| **Timetable** | Aktiv | `TimetableArchive`, `TimetableRowData`, Routing- und TTT-Enums | `TimetableBuilderView`, `TimetableMap` | Full-screen Builder in 2 Schritten, OSM-Karte, kuerzester Weg ueber SoLs, Archivierung ueber `CAPACITY`-Bedarf |
| **Infrastructure** | Import aktiv | `OperationalPoint`, `SectionOfLine`, `ImportLog`, `PredefinedTag` | `SettingsView`, `TagsTab`, `TopologyTab` | ERA RINF: 12.298 OPs + 13.849 SoLs (CH+DE), CSV-Import fuer Schlagwoerter, synthetische Grenzverbinder fuer Routing |
| **Customer** | Entity | `Customer`, `CustomerStatus` | — | Repository vorhanden, eigene UI noch offen |
| **Business** | Entity | `Business`, `BusinessStatus` | — | Repository vorhanden, eigene UI noch offen |

## Infrastruktur

| Komponente | Status | Details |
|---|---|---|
| **Vaadin** | 24.7.4 | Aktueller UI-Stand fuer Orders, Settings und Builder |
| **Security (Keycloak)** | Produktionsreif | OIDC + Realm Role Mapping, `@RolesAllowed` aktiv |
| **Rollen** | Verifiziert | `ADMIN` fuer Settings, Dispatcher und Viewer ohne Admin-Rechte |
| **i18n** | Aktiv | DE/EN/IT/FR, inkl. Builder, Settings, Order-Views |
| **Push / Live Updates** | Skeleton | `BroadcastService` und `@Push` vorhanden |
| **Audit Trail** | Aktiv | Hibernate Envers fuer Orders, Positionen, Ressourcen, Archive |
| **Datenbank** | V1-V7 Migrationen | Orders, Positionen, Ressourcen, Bestellungen, Infrastruktur, Schlagwoerter, Positionskommentare, Fahrplanarchiv |
| **Theme** | Aktiv | Profile-basiertes Theme mit sofortigem Umschalten; Fallback auf Default abgesichert |
| **Accessibility** | Aktiv | ARIA-Labels, Tastaturbedienbarkeit, lesbare Statuschips, Builder und Liste testbar |
| **Laufzeit** | Lokal verifiziert | Anwendung laeuft ohne Docker auf `*:8085` |

## Quality Gates

| Tool | Status | Beschreibung |
|---|---|---|
| **Spotless** | Aktiv | Google Java Style (AOSP) |
| **ArchUnit** | Aktiv | DDD-Schichtregeln, Naming, Annotations |
| **JaCoCo** | Aktiv | Mindest-Coverage im Build |
| **SpotBugs** | Aktiv | Statische Analyse |
| **OWASP Dep Check** | Aktiv | CVE-Scanning bei PRs |
| **Gitleaks** | Aktiv | Secret Scanning |
| **Playwright** | Aktiv | E2E fuer Login-/Order-/Builder-Pfade, inkl. Fahrplanbuilder-Regression |

## UI

| View | Route | Status | Beschreibung |
|---|---|---|---|
| **MainLayout** | — | Implementiert | Drawer-Nav, Breadcrumbs, Sprach- und Theme-Kontext |
| **LoginView** | `/login` | Implementiert | Keycloak SSO Redirect |
| **DashboardView** | `/` | Implementiert | KPI-Cards (Platzhalter) |
| **OrderListView** | `/orders` | Implementiert | Accordion + Summary-Metriken + Kommentarzeile + Status-Chips mit Zaehlern und Positionsfilter |
| **OrderDetailView** | `/orders/{id}` | Implementiert | Kompakter Header + Auftragspositionen + Bestellkalender |
| **TimetableBuilderView** | `/orders/{orderId}/timetable-builder` | Implementiert | Full-screen Fahrplanbuilder mit Route, Karte, Tabelle und Archiv-Save |
| **SettingsView** | `/settings` | Implementiert | Topologie-Import + Schlagwort-Katalog + Datenbestand + Import-Verlauf (ADMIN only) |

## UI-Komponenten

| Komponente | Beschreibung |
|---|---|
| `OrderFormPanel` | Auftragsformular mit vordefiniertem Schlagwort-Katalog fuer `ORDER` / `GENERAL` |
| `ServicePositionDialog` | Dialog fuer `LEISTUNG`-Positionen mit OP-Auswahl, Zeitfeldern, Gueltigkeit, Tags und Kommentar |
| `OrderPositionPanel` | Einstieg fuer neue `LEISTUNG`- und `FAHRPLAN`-Positionen |
| `OrderPositionRow` | Angereicherte Positionszeile in der Auftragsbearbeitung mit Route, Zeitfenster, Service-Typ, Tags und Kommentar |
| `PositionTile` | Positions-Kachel in der Auftragsliste mit Route, Zeitfenster, Kommentar, Tags, Bestellanzahl und Status |
| `PurchaseCalendarPanel` | Bestellkalender mit Summary, Grid und Details pro Position |
| `TimetableMap` | Leaflet-/OpenStreetMap-Komponente fuer die Fahrplanroute |
| `TagsTab` | Schlagwort-Katalog mit CSV-Import im Settings-Bereich |
| `TopologyTab` | RINF-Import und Datenbestand im Settings-Bereich |

## Datenbank-Migrationen

| Version | Datei | Inhalt |
|---|---|---|
| V1 | `V1__create_schema.sql` | `orders`, `orders_audit`, `revinfo` |
| V2 | `V2__expand_domain_model.sql` | `customers`, `order_positions`, `resource_needs`, `purchase_positions`, `businesses` + Audit |
| V3 | `V3__infrastructure_tables.sql` | `operational_points`, `sections_of_line`, `import_log` |
| V4 | `V4__simplify_orders.sql` | `validFrom` / `validTo` statt Fahrplanjahr-Label |
| V5 | `V5__predefined_tags.sql` | `predefined_tags` Tabelle fuer Schlagwort-Katalog |
| V6 | `V6__position_comment.sql` | Kommentarspalte fuer `order_positions` und Audit |
| V7 | `V7__timetable_archive_and_border_connectors.sql` | `timetable_archives`, Audit, FK von `resource_needs`, 0m-Grenzverbinder fuer CH/DE-Routing |

## ERA RINF Infrastrukturdaten

| Land | Operational Points | Sections of Line |
|---|---|---|
| Schweiz (CHE) | 3.261 | 1.588 |
| Deutschland (DEU) | 9.037 | 12.261 |
| **Total** | **12.298** | **13.849** |

Quelle: ERA SPARQL Endpoint `https://era.linkeddata.es/sparql`

Hinweise:

- `operational_points_de.csv` enthaelt 16.060 Rohzeilen; der Import dedupliziert vor dem Persistieren nach `uopid`
- Fuer den aktuellen Fahrplanbuilder wurden vier synthetische Grenzverbinder mit `0m` Laenge eingefuegt, damit Basel Bad Bf, Schaffhausen, Konstanz und Kreuzlingen im CH/DE-Graph routbar bleiben

## Offene TODOs

- [ ] Customer Views
- [ ] Business-/Geschaeft-Views
- [ ] Fahrplanversionierung statt 1:1-Archivbeziehung
- [ ] TTT-Export / Versand fuer `tttRelevant` Fahrplanzeilen
- [ ] Lazy Loading / Pagination bei grossen Datenmengen
- [ ] Erweiterte E2E-Tests fuer Fehlerszenarien und Imports

## Bekannte Issues

- Routing ist bewusst datengetrieben: wenn im geladenen OP-/SoL-Graph ein Segment fehlt, blockiert der Fahrplanbuilder mit `No route found`
- Repo-weit existieren noch Formatabweichungen fuer `spotless:check`, die nicht funktional am aktuellen Feature haengen

## Architektur-Entscheidungen (ADRs)

1. **ADR-001**: Spring Security OAuth2 statt Keycloak Adapter
2. **ADR-002**: Flyway statt Liquibase
3. **ADR-003**: Hibernate Envers fuer Audit Trail
4. **ADR-004**: Accordion + Status-Chips als Primaransicht fuer Auftraege
5. **ADR-005**: Kompakter Order-Header + Dialoge/Fullscreen statt ueberladener Einzelseite
6. **ADR-006**: Bestellkalender mit TTR-Phasen
7. **ADR-007**: ERA RINF als Infrastruktur-Stammdaten
8. **ADR-008**: CSV-Import fuer Schlagwort-Katalog statt SQL-Seed
9. **ADR-009**: `FAHRPLAN`-Positionen speichern den Detailfahrplan nicht selbst, sondern verlinken 1:1 auf `timetable_archives` ueber einen `CAPACITY`-Ressourcenbedarf

## Changelog

| Datum | Aenderung |
|---|---|
| 2026-03-31 | Dokumentation fuer `LEISTUNG`- und `FAHRPLAN`-Positionen, Builder, Archiv und aktuelle UI konsolidiert |
| 2026-03-31 | Fahrplanbuilder umgesetzt: 2 Schritte, OSM-Karte, kuerzester Weg ueber SoLs, TTT-nahe Zeitmodi, Archivspeicherung |
| 2026-03-31 | `timetable_archives` + CH/DE-Grenzverbinder per V7 eingefuehrt |
| 2026-03-31 | `LEISTUNG`-Dialog um OP-Auswahl, Pflicht-Zeitfelder und Kommentar erweitert |
| 2026-03-31 | Auftragsliste neu gestaltet: Summary-Metriken, Kommentarzeile, Status-Chips mit Zaehlern und Positionsfilter |
| 2026-03-31 | Schlagwort-Katalog aus SQL-Seed entfernt, CSV-Quelle `data/seeds/predefined-tags.csv`, Import im Settings-Bereich |
| 2026-03-31 | RINF-Import gehaertet: atomarer Replace-Import, DE Operational Points deduplizieren nach `uopid` |
| 2026-03-31 | Theme-Anwendung abgesichert und sofortiger Profil-Themewechsel repariert |
| 2026-03-31 | Playwright-E2E fuer Fahrplanbuilder ergaenzt |
