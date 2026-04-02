# Projektgedaechtnis — Railway Order Management

> Dieses Dokument wird nach groesseren funktionalen Aenderungen mit dem aktuellen Ist-Stand nachgezogen.

## Letzte Aktualisierung

**2026-03-31** — Path Manager Dokumentation umfassend erweitert: datenmodel.md mit ER-Diagramm, detaillierten Feldtabellen (alle 8 Entities), vollstaendiger TTT State Machine (Mermaid stateDiagram), Zustandsbeschreibungen, Aktionen/Rollen-Tabelle, Integrationsmodell (Datenfluss-Diagramm) und REST-API-Endpunkt-Tabelle. ARCHITECTURE.md mit Komponenten-Diagramm, PathProcessEngine-Pattern-Beschreibung und API-Summary. Wiki-Seite docs/wiki/path-manager.md (User Guide mit TreeGrid-Erklaerung, Submissions-Anleitung, 3 Beispielflows, Zugkopf/OP-Bearbeitung, Diff-Feature, API-Referenz). ADR-011 (Path Manager Architektur: REST API als Integrations-Boundary, Shared DB mit pm_-Prefix, statische State Machine, Simulationsansatz, Alternativen: Microservice/Direct Injection/Message Queue). GLOSSARY.md um 16 Path-Manager-Begriffe erweitert (Fahrplanmanager, Referenzzug, Trasse, Trassenantrag, Laufweg, Zugversion, Prozessschritt, State Machine, RA, IM, Draft/Final Offer, Booked, Fahrplanjahr, TRID, PAID, PRID, ROID).

Davor: Path Manager Integration Button: "Send to Path Manager" Button in `OrderPositionRow` fuer FAHRPLAN-Positionen (Train-Icon, orange wenn nicht gesendet, teal wenn gesendet). `OrderPositionPanel` ruft `PathManagerService.createTrainFromOrderPosition()` auf, speichert `pmReferenceTrainId` auf der Position, zeigt Erfolgs-Notification. Nach Senden wechselt der Button zu "View in PM" und navigiert zum PathManagerView. Neue Dependencies in `OrderDetailView` durchgereicht. i18n in DE/EN/IT/FR. Doku in datenmodel.md, timetable-builder.md und ARCHITECTURE.md aktualisiert.

Davor: Path Manager Vaadin UI umgesetzt: `PathManagerView` mit SplitLayout (40/60), TreeGrid mit sealed-interface `TreeNode` (YearNode, TrainNode, VersionNode, LocationNode) und lazy-loading DataProvider. Rechte Seite kontextabhaengig: `TrainHeaderPanel` (Zugdaten-Formular), `ProcessSimulationPanel` (TTT State Machine mit RA/IM-farbcodierten Aktionsbuttons, Kommentarfeld, Prozesshistorie-Grid), `JourneyLocationPanel` (OP-Bearbeitung), Versionsdetail mit Location-Grid. Navigation in MainLayout ergaenzt (VaadinIcon.TRAIN), Breadcrumb-Support. i18n in DE/EN/IT/FR (40+ Keys).

Davor: Path Manager REST API umgesetzt: 9 DTOs in `dto.pathmanager` (TrainSubmitRequest, JourneyLocationDto, TrainSummaryDto, TrainDetailDto, TrainVersionDto, ProcessTransitionRequest, ProcessStepDto, AvailableActionsDto, DiffResultDto), Mapper in `mapper.pathmanager` (PathManagerDtoMapper), 3 REST Controller in `api.pathmanager` (PathManagerController mit 7 Endpoints, PathProcessController mit 3 Endpoints, PathManagerDiffController mit 1 Endpoint). Springdoc/Swagger annotiert. Kompiliert sauber, Spotless angewendet.

Davor: Path Manager Domain Layer komplett umgesetzt: V9-Migration mit 8 `pm_*` Tabellen + Audit-Tabellen + Indexes, FK-Erweiterung auf `order_positions`, Seed-Daten fuer FPJ 2026. Entities (`PmReferenceTrain`, `PmTrainVersion`, `PmJourneyLocation`, `PmPath`, `PmPathRequest`, `PmRoute`, `PmTimetableYear`, `PmProcessStep`), 4 Enums (`PathProcessState`, `PathAction`, `PathProcessType`, `VersionType`), 8 Repositories, 4 Services (`PathProcessEngine` mit vollstaendiger TTT State Machine, `PathManagerService` CRUD, `IdentifierGenerator`, `DiffService`).

Davor: Read-only Timetable Archive View (Fahrplan-Detailansicht) unter Route `orders/:orderId/timetable/:positionId`. SplitLayout 65/35: links Div-basierte Fahrplantabelle mit Farbcodierung (Origin/Destination amber, Halte teal, Durchfahrten gedaempft, Soft-Delete durchgestrichen), rechts Karte + Gueltigkeit + Metadaten. View-Button (Auge) in OrderPositionRow fuer FAHRPLAN-Positionen. i18n in DE/EN/IT/FR.

Davor: OTN-Feld (Operational Train Number) dokumentiert: Freitext VARCHAR 20 auf `timetable_archives` und `order_positions`, V8-Migration, Anzeige in Positionslisten/-kacheln, TTT-Mapping.

Davor: Fahrplanbuilder Phase 2-4: Timetable-Editing Features implementiert. TimetableTableStep erneut aufgeteilt (888 -> 367+507+140 Zeilen): AddStopForm und TimetableRowEditorPanel als eigene Klassen extrahiert. Stop Add/Remove mit Inline-Form und Soft-Delete, Shift/Stretch-Zeitpropagation, Commercial-Zeitmodus (PLA/PLD), TimingQualifier-Tags, Pinned-Zeiten, Activity-Pflicht bei Halt mit visueller Hervorhebung.

## Projektstatus

**Order CRUD + Auftragspositionen (`LEISTUNG` / `FAHRPLAN`) + Fahrplanbuilder + Bestellkalender + RINF Import + Schlagwort-Katalog**

## Module / Bounded Contexts

| Modul | Status | Entities | Views / Komponenten | Bemerkung |
|---|---|---|---|---|
| **Order** | CRUD aktiv | `Order`, `OrderPosition`, `ResourceNeed`, `PurchasePosition` + Status-/Typ-Enums | `OrderListView`, `OrderDetailView`, `OrderFormPanel`, `OrderPositionPanel`, `ServicePositionDialog` | Auftragsuebersicht mit Status-Chips, Detailansicht mit angereicherten Positionszeilen, Leistungspositionen als Dialog |
| **Timetable** | Aktiv | `TimetableArchive`, `TimetableRowData`, `TimetableEditingService`, Routing- und TTT-Enums | `TimetableBuilderView`, `TimetableRouteStep`, `TimetableTableStep`, `TimetableRowEditorPanel`, `AddStopForm`, `TimetableFormatUtils`, `TimetableMap` | Builder mit Stop Add/Remove (Inline-Form + Soft-Delete), Shift/Stretch-Propagation, Commercial-Modus (PLA/PLD), TimingQualifier-Tags, Pinned-Zeiten |
| **Infrastructure** | Import aktiv | `OperationalPoint`, `SectionOfLine`, `ImportLog`, `PredefinedTag` | `SettingsView`, `TagsTab`, `TopologyTab` | ERA RINF: 12.298 OPs + 13.849 SoLs (CH+DE), CSV-Import fuer Schlagwoerter, synthetische Grenzverbinder fuer Routing |
| **Customer** | Entity | `Customer`, `CustomerStatus` | — | Repository vorhanden, eigene UI noch offen |
| **Business** | Entity | `Business`, `BusinessStatus` | — | Repository vorhanden, eigene UI noch offen |
| **Path Manager** | Domain + REST API + UI + Integration | `PmReferenceTrain`, `PmTrainVersion`, `PmJourneyLocation`, `PmPath`, `PmPathRequest`, `PmRoute`, `PmTimetableYear`, `PmProcessStep` + 4 Enums | `PathManagerView`, `TrainHeaderPanel`, `ProcessSimulationPanel`, `JourneyLocationPanel`, `TreeNode` | TTT State Machine, CRUD, Diff, Identifier-Generator. V9-Migration. REST API: 11 Endpoints. UI: TreeGrid + Detail Panels mit Prozess-Simulation. Integration: Send-to-PM Button in OrderPositionRow. |

## Infrastruktur

| Komponente | Status | Details |
|---|---|---|
| **Vaadin** | 24.7.4 | Aktueller UI-Stand fuer Orders, Settings und Builder |
| **Security (Keycloak)** | Produktionsreif | OIDC + Realm Role Mapping, `@RolesAllowed` aktiv |
| **Rollen** | Verifiziert | `ADMIN` fuer Settings, Dispatcher und Viewer ohne Admin-Rechte |
| **i18n** | Aktiv | DE/EN/IT/FR, inkl. Builder, Settings, Order-Views |
| **Push / Live Updates** | Skeleton | `BroadcastService` und `@Push` vorhanden |
| **Audit Trail** | Aktiv | Hibernate Envers fuer Orders, Positionen, Ressourcen, Archive |
| **Datenbank** | V1-V8 Migrationen | Orders, Positionen, Ressourcen, Bestellungen, Infrastruktur, Schlagwoerter, Positionskommentare, Fahrplanarchiv, OTN |
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
| **TimetableArchiveView** | `/orders/{orderId}/timetable/{positionId}` | Implementiert | Read-only Fahrplan-Detailansicht mit Div-Tabelle, Karte, Gueltigkeit und Metadaten |
| **PathManagerView** | `/pathmanager` | Implementiert | SplitLayout 40/60: TreeGrid (Year/Train/Version/Location) + kontextabhaengige Detail-Panels (Zug-Header, Prozess-Simulation, OP-Bearbeitung) |
| **SettingsView** | `/settings` | Implementiert | Topologie-Import + Schlagwort-Katalog + Datenbestand + Import-Verlauf (ADMIN only) |

## UI-Komponenten

| Komponente | Beschreibung |
|---|---|
| `OrderFormPanel` | Auftragsformular mit vordefiniertem Schlagwort-Katalog fuer `ORDER` / `GENERAL` |
| `ServicePositionDialog` | Dialog fuer `LEISTUNG`-Positionen mit OP-Auswahl, Zeitfeldern, Gueltigkeit, Tags und Kommentar |
| `OrderPositionPanel` | Einstieg fuer neue `LEISTUNG`- und `FAHRPLAN`-Positionen |
| `OrderPositionRow` | Angereicherte Positionszeile in der Auftragsbearbeitung mit Route, Zeitfenster, Service-Typ, Tags, Kommentar, View-Button (Auge) und Send-to-PM-Button (Zug) fuer FAHRPLAN |
| `PositionTile` | Positions-Kachel in der Auftragsliste mit Route, Zeitfenster, Kommentar, Tags, Bestellanzahl und Status |
| `PurchaseCalendarPanel` | Bestellkalender mit Summary, Grid und Details pro Position |
| `TimetableRowEditorPanel` | Rechte Seite des Table-Steps: Zeitbearbeitung, Halt/Aktivitaet, Propagation, Commercial-Modus |
| `AddStopForm` | Inline-Formular zum Hinzufuegen eines neuen Halts nach einer Zeile |
| `TimetableArchiveTable` | Read-only Div-basierte Fahrplantabelle mit Farbcodierung fuer Origin/Destination, Halte, Durchfahrten und Soft-Delete |
| `TimetableArchiveSidebar` | Rechte Seite der Archivansicht: Karte, Gueltigkeit, Metadaten |
| `TimetableMap` | Leaflet-/OpenStreetMap-Komponente fuer die Fahrplanroute |
| `TagsTab` | Schlagwort-Katalog mit CSV-Import im Settings-Bereich |
| `TopologyTab` | RINF-Import und Datenbestand im Settings-Bereich |
| `TreeNode` | Sealed-Interface mit YearNode, TrainNode, VersionNode, LocationNode fuer heterogenen TreeGrid |
| `TrainHeaderPanel` | Editierbares Formular fuer Zugdaten (OTN, Typ, Gewicht, Laenge, Geschwindigkeit, Kalender) |
| `ProcessSimulationPanel` | TTT-Prozesssimulation: Status-Badge, RA/IM-farbcodierte Aktionsbuttons, Kommentar, Prozesshistorie-Grid |
| `JourneyLocationPanel` | Editierbares Formular fuer Betriebspunkte (Zeiten, Qualifier, Aktivitaeten, Gleis) |

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
| V8 | `V8__timetable_otn.sql` | `operational_train_number` (VARCHAR 20, nullable) auf `timetable_archives` und `order_positions` |
| V9 | `V9__path_manager_tables.sql` | `pm_timetable_years`, `pm_reference_trains`, `pm_routes`, `pm_path_requests`, `pm_paths`, `pm_train_versions`, `pm_journey_locations`, `pm_process_steps` + Audit-Tabellen + FK `pm_reference_train_id` auf `order_positions` + Seed FPJ 2026 |

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
- [ ] E2E-Test fuer Timetable Archive View im Order-Lifecycle

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
10. **ADR-010**: Shift/Stretch-Zeitpropagation mit Pin-Konzept fuer Fahrplan-Zeitbearbeitung
11. **ADR-011**: Path Manager Architektur — REST API als Integrations-Boundary, Shared DB mit pm_-Prefix, statische State Machine

## Changelog

| Datum | Aenderung |
|---|---|
| 2026-03-31 | Path Manager Doku erweitert: datenmodel.md (ER-Diagramm, Feldtabellen, State Machine, Integration), ARCHITECTURE.md (Komponenten, API), wiki/path-manager.md (User Guide), ADR-011, GLOSSARY.md (+16 Begriffe) |
| 2026-03-31 | Path Manager Integration: Send-to-PM Button in OrderPositionRow, OrderPositionPanel Handler mit PathManagerService/TimetableArchiveService, OrderDetailView DI. i18n DE/EN/IT/FR. Doku: datenmodel.md, timetable-builder.md, ARCHITECTURE.md |
| 2026-03-31 | Path Manager Vaadin UI: PathManagerView (TreeGrid + SplitLayout), TrainHeaderPanel, ProcessSimulationPanel, JourneyLocationPanel, TreeNode sealed interface. Navigation + Breadcrumb. i18n DE/EN/IT/FR |
| 2026-03-31 | Path Manager REST API: 9 DTOs, PathManagerDtoMapper, 3 Controller (PathManagerController 7 Endpoints, PathProcessController 3 Endpoints, PathManagerDiffController 1 Endpoint). Springdoc annotiert |
| 2026-03-31 | Path Manager Domain Layer: V9-Migration (8 Tabellen + Audit), 8 Entities, 4 Enums, 8 Repositories, 4 Services (State Machine, CRUD, Diff, Identifier), FK auf order_positions |
| 2026-03-31 | E2E-Test und Dokumentation fuer Timetable Archive View: Lifecycle-Test Schritt 3b, Datenmodell-Route, Wiki-Abschnitt, ARCHITECTURE.md Komponentenhierarchie |
| 2026-03-31 | Read-only Timetable Archive View: SplitLayout 65/35, Div-basierte Fahrplantabelle mit Farbcodierung, Karte, Gueltigkeit, Metadaten. View-Button in OrderPositionRow. i18n DE/EN/IT/FR |
| 2026-03-31 | Doku: OTN-Feld (Operational Train Number) in datenmodel.md, timetable-builder.md und PROJECT_STATUS.md dokumentiert. Freitext VARCHAR 20 auf `timetable_archives` und `order_positions`, V8-Migration |
| 2026-03-31 | Doku: datenmodel.md (neue TimetableRowData-Felder, Enums, Editing-Service), ARCHITECTURE.md (Komponentenhierarchie, Propagationsarchitektur), ADR-010 (Shift/Stretch), Wiki timetable-builder.md, GLOSSARY.md (6 neue Begriffe) |
| 2026-03-31 | Timetable-Editing Phase 2-4: Stop Add/Remove, Shift/Stretch-Propagation, Commercial-Modus, TimingQualifier-Tags, Pinned-Zeiten. TimetableTableStep in 3 Klassen aufgeteilt |
| 2026-03-31 | Dokumentation fuer `LEISTUNG`- und `FAHRPLAN`-Positionen, Builder, Archiv und aktuelle UI konsolidiert |
| 2026-03-31 | Fahrplanbuilder umgesetzt: 2 Schritte, OSM-Karte, kuerzester Weg ueber SoLs, TTT-nahe Zeitmodi, Archivspeicherung |
| 2026-03-31 | `timetable_archives` + CH/DE-Grenzverbinder per V7 eingefuehrt |
| 2026-03-31 | `LEISTUNG`-Dialog um OP-Auswahl, Pflicht-Zeitfelder und Kommentar erweitert |
| 2026-03-31 | Auftragsliste neu gestaltet: Summary-Metriken, Kommentarzeile, Status-Chips mit Zaehlern und Positionsfilter |
| 2026-03-31 | Schlagwort-Katalog aus SQL-Seed entfernt, CSV-Quelle `data/seeds/predefined-tags.csv`, Import im Settings-Bereich |
| 2026-03-31 | RINF-Import gehaertet: atomarer Replace-Import, DE Operational Points deduplizieren nach `uopid` |
| 2026-03-31 | Theme-Anwendung abgesichert und sofortiger Profil-Themewechsel repariert |
| 2026-03-31 | Playwright-E2E fuer Fahrplanbuilder ergaenzt |
