# E2E-Test-Backlog

Stand der echten Klicktest-Suite (`e2e/clicktest-*.spec.ts`, Fundament `e2e/fixtures.ts`)
und was noch offen ist. Grundlage: Audit (2 Review-Agenten + Codex, 2026-06-28).

Prinzipien der neuen Suite: **echte Klicks/Eingaben** (keine `grid.items`/`page.evaluate`/REST in
Assertions), **Browser-Konsolen-/Pageerror-Guard** über `fixtures.ts` (jeder JS-Fehler lässt den
Test scheitern), **Screenshots** zur Sichtprüfung, **selbst-aufräumend** (per UI angelegte Daten
werden per UI wieder gelöscht).

## Abgedeckt (grün)
- **Validierung**: leerer New Order / New Business / New rule / Add Vehicle.
- **Order-Formular-Regeln** (`clicktest-order-form.spec.ts`): `validTo < validFrom` wird beim Save
  abgelehnt (Feld invalid + Text „Valid To < Valid From", keine Navigation, `OrderFormPanel:168`);
  **Kostenträger-Pflicht bei FREIGEGEBEN** (SOB §5.7, `OrderService:167`) — Wechsel der
  Bearbeitungs-Status-Select auf „Approved" ohne Kostenträger → Fehler-Notification, Status nicht
  persistiert (selbst-aufräumend per UI-Delete).
- **CAPACITY-Einkauf → TttOrderDialog** (`clicktest-ttt-order.spec.ts`): auf Seed-Auftrag 3234
  (FAHRPLAN/CAPACITY-Position) „+ Add Purchase Position" → Save (TTT vorausgewählt) → TttOrderDialog;
  leerer Submit flaggt alle 6 Pflichtfelder (`[invalid]`, KEINE Notification), ungültige E-Mail wird
  beim Blur abgelehnt, gültige löscht den Fehler (`TttOrderDialog.validateRequired`). Hinweis: legt
  eine OFFENE CAPACITY-Bestellung auf 3234 an (kein UI-Delete für Einkäufe) → DB-Teardown:
  `delete … purchase_positions … 3234 … resource_type='CAPACITY' and pm_path_request_id is null`.
- **CRUD pur per Klick**: Business create→read→edit→delete; Order create→read→delete.
- **Navigation/Views**: jede View server- + konsolen-fehlerfrei, Sidenav-Klick, Dashboard,
  Order→Positionsdetail→zurück, Open Positions, Settings-Tabs, Profile, R2P-Simulate.
- **Filter**: Order-Übersichts-Suche.
- **Dialoge**: AuditHistory öffnen/schließen; ServicePosition leerer Name; (R2P-Accept, TttOrder,
  Resource — siehe Fortschritt unten).
- **Fahrplan-Builder Route-Validierung** (`clicktest-timetable.spec.ts`, 5 grün): Route ohne
  Start/Ziel, gleicher OP („Origin and destination must be different"), beide Anker gesetzt („set
  only one anchor time, not both"), unvollständiger Via-Punkt („complete every via point"), Via als
  „Stop" ohne Aktivität („Each intermediate stop requires an activity") — alle über die
  `routeError`-Span (`body.toContainText`, KEINE Notification), OP-Auswahl per getByLabel-Combo +
  `vaadin-combo-box-item` (Olten/Aarau, RINF-Seed); der label-lose Via-Combo wird über den
  `<div>`-Container der „Stop"-Checkbox gescopt.

## Gefundene Bugs (per Klicktest)
- **(GEFIXT) Order-Übersicht: Validitäts-Datumsfilter (From/To) filterte die Liste nicht.**
  Ursache war NICHT die UI: `searchOrders` enthielt `(:validFromMin is null or o.validTo >=
  :validFromMin)`. Für den `LocalDate`-Bind erzeugte Hibernate ein nacktes `? is null` ohne
  Typ-Kontext; PostgreSQL scheiterte beim Parse mit `42P18` („konnte Datentyp von Parameter $10
  nicht ermitteln"). Die InvalidDataAccessResourceUsageException wurde vom Lazy-Loader
  verschluckt → die Liste behielt den alten (ungefilterten) Inhalt, der Chip war aber gesetzt.
  Fix: den Null-Guard casten — `cast(:validFromMin as LocalDate) is null` (analog zum bereits
  vorhandenen `cast(:text as string)`), gleiches für `:validToMax`. Regressionsschutz:
  `LazyListSearchQueryTest.searchOrders_validityRange_filtersAndDoesNotThrow` (echtes Postgres) +
  Klicktest `clicktest-order-filters.spec.ts` „Validity 'From' filter…" (nicht mehr `fixme`).
- **(GEFIXT) TttOrderDialog: fehlende i18n-Keys** `ttt.order.trainType`/`.trafficType` (+ `.help`)
  in EN/IT/FR → die Combo-Labels rendern als `!ttt.order.trainType!` / `!ttt.order.trafficType!`
  (DE hatte die Keys). Fix: Keys in messages_en/it/fr ergänzt (Parität zu DE). Guard:
  `clicktest-ttt-order.spec.ts` prüft, dass die Marker fehlen und „Train type"/„Traffic type" da sind.
- (früher gefunden, bereits gefixt) Filter-Chip-✕ rief Record-Accessor `clear()` ohne `.run()`.

## Offen

### Groß (eigenes Paket) — `sbahn-integration.spec.ts` als echte Klicktests neu schreiben
Heute der dichteste Test (~2077 Z.), aber durchgehend „DB-Magie": `grid.items` lesen, Feld-`.value`
per `evaluate` setzen, Zeilen ohne Klick selektieren, Assertions auf das Komponentenmodell statt DOM
(das ist auch der einzige Fehlschlag der Suite: **sbahn-05a** `readRowData`). Umfang: 20+ serielle
Schritte (Routenberechnung, Zwischenhalte/Dwell, Zeitmodi WINDOW/AFTER/BEFORE/EXACT + Mirroring,
Tag-Offset, Soft-Delete/Undo, Intervall-Generierung 38 Positionen, an Path-Manager senden, Archiv,
Ressource, Bestellung, TTT-Dialog). Der Fahrplan-Grid mit Inline-Editor + Zeitpickern ist die am
schwersten per Klick steuerbare UI; jede Assertion muss auf gerendertes DOM umgestellt werden.
Schätzung **1–2 Tage**, stabil/nicht-flaky.

### Mittel
- **`path-manager.spec.ts`** von REST auf echte Tree-UI: expandieren → Train wählen → Prozess-Button
  → Status im DOM → Planning-Status/Train-Header speichern.

### Klein
- **ResourceDialog (leere Verkehrstage)**: Scout-Befund — als reiner Klicktest **nicht empfohlen**.
  Der Dialog-Einstieg „Add Need" erscheint nur, wenn die Position bereits einen Resource-Need hat,
  und es gibt keine geseedeten Needs → man müsste erst über den Fahrplan-Builder-Wizard einen Need
  erzeugen (lang/fragil). Besser als Unit-Test auf `OperatingDays.of` / `ResourceDialog.saveResource`
  oder angehängt an den bestehenden sbahn-Flow.
- **`order-crud.spec.ts` / `debug-page.spec.ts`**: 0 Assertions → reparieren oder entfernen.
- **TimetableArchiveView**: Eye-Button aus Positionszeile, Back/Edit, Guard für non-FAHRPLAN.
- **ExpressionVerkehrstage- / UnassignedTrains-Dialog**.
- **Profile-Save + Restore**, **Settings-CSV-Import** (success/error, braucht Test-Datei).

### Blocker (nicht reiner Aufwand)
- **Rollen-Gates** (unautorisierter Zugriff auf ADMIN-Views) brauchen einen **zweiten Keycloak-
  Account ohne Admin-Rechte** — muss bereitgestellt werden.
- **`/customers` ist ein toter Nav-Link** (keine View) — Produktentscheid: View bauen oder Link
  entfernen, dann testen.
