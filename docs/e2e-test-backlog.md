# E2E-Test-Backlog

Stand der echten Klicktest-Suite (`e2e/clicktest-*.spec.ts`, Fundament `e2e/fixtures.ts`)
und was noch offen ist. Grundlage: Audit (2 Review-Agenten + Codex, 2026-06-28).

Prinzipien der neuen Suite: **echte Klicks/Eingaben** (keine `grid.items`/`page.evaluate`/REST in
Assertions), **Browser-Konsolen-/Pageerror-Guard** über `fixtures.ts` (jeder JS-Fehler lässt den
Test scheitern), **Screenshots** zur Sichtprüfung, **selbst-aufräumend** (per UI angelegte Daten
werden per UI wieder gelöscht).

## Abgedeckt (grün)
- **Validierung**: leerer New Order / New Business / New rule / Add Vehicle.
- **CRUD pur per Klick**: Business create→read→edit→delete; Order create→read→delete.
- **Navigation/Views**: jede View server- + konsolen-fehlerfrei, Sidenav-Klick, Dashboard,
  Order→Positionsdetail→zurück, Open Positions, Settings-Tabs, Profile, R2P-Simulate.
- **Filter**: Order-Übersichts-Suche.
- **Dialoge**: AuditHistory öffnen/schließen; ServicePosition leerer Name; (R2P-Accept, TttOrder,
  Resource — siehe Fortschritt unten).
- **Fahrplan-Builder**: Route ohne Start/Ziel → Fehler.

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
- **Order-Filterpanel komplett**: Status, interner Status, Auftragstyp, Datumsbereich, Tags,
  „assigned to me", „incomplete" (bisher nur Freitext-Suche).
- **Dialog-Validierungen Rest**: ResourceDialog (leere Verkehrstage bei beschränkten Betriebstagen),
  PurchaseDialog→TttOrderDialog kompletter CAPACITY-Flow (Pflichtfelder Debitcode/TrainType/
  TrafficType/Contact/Brake + ungültige E-Mail).
- **`validTo < validFrom`** und **Kostenträger-Pflicht bei FREIGEGEBEN** (Status-Transition).

### Klein
- **`order-crud.spec.ts` / `debug-page.spec.ts`**: 0 Assertions → reparieren oder entfernen.
- **TimetableArchiveView**: Eye-Button aus Positionszeile, Back/Edit, Guard für non-FAHRPLAN.
- **ExpressionVerkehrstage- / UnassignedTrains-Dialog**.
- **Profile-Save + Restore**, **Settings-CSV-Import** (success/error, braucht Test-Datei).

### Blocker (nicht reiner Aufwand)
- **Rollen-Gates** (unautorisierter Zugriff auf ADMIN-Views) brauchen einen **zweiten Keycloak-
  Account ohne Admin-Rechte** — muss bereitgestellt werden.
- **`/customers` ist ein toter Nav-Link** (keine View) — Produktentscheid: View bauen oder Link
  entfernen, dann testen.
