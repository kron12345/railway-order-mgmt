# Plan: Intake-Politur & SOB-Kandidaten (Follow-up)

**Stand:** 2026-06-24 · **Branch:** `feature/railopt-demo-prep` · Status: FINAL (Umfang per User geklärt)

Folge-Plan nach dem 5-Slice-Intake-Plan (`order-positions-intake-plan.md`, A–E fertig & gepusht). Umfang per User-Entscheid: **Intake-Politur + 3 SOB-Kandidaten**; Aufräumen = nur E2E-Demo-Daten zurücksetzen; `sebastian`-Rechte erstmal nicht; Perf-Slice 5 NICHT.

Arbeitsmodus (wie gehabt): pro Slice ein Commit, Compile + Spotless + ArchUnit grün, Migrationen in Rollback-Tx validiert, Codex-Review je Commit (Findings beheben), am Ende Playwright-E2E als `dispatcher` + Push.

---

## Slice F — Jahresbestellung-Demodaten (Badge sichtbar)
**Problem:** Alle Seed-Aufträge haben `validFrom ≈ createdAt` → `OrderType` ist immer EINZELBESTELLUNG; der JAHRESBESTELLUNG-Badge (Slice C) ist nie zu sehen.
**Lösung:** Flyway-Seed `V37` legt einen Demo-Auftrag mit großem Vorlauf an: `created_at` ≈ 12 Monate vor `valid_from` (z.B. created 2025-06-01, gültig 2026-06-01 … 2026-12-12) → Lead ≥ 2 Mt → `OrderType.JAHRESBESTELLUNG`. 1–2 FAHRPLAN-Positionen für Realismus. Order-Nummer eindeutig (z.B. `JB-2026-001`).
**Dateien:** `V37__seed_jahresbestellung_demo.sql`. **Risiko:** niedrig.

## Slice G — „Aus RailOpt"-Positionen anreichern (Archiv + CAPACITY)
**Problem:** Per „Aus RailOpt" erfasste FAHRPLAN-Positionen sind dünn — kein `TimetableArchive`, keine CAPACITY-`ResourceNeed`; „Fahrplan öffnen" springt zurück, nicht TTT-bestellbar.
**Lösung:** In `PathManagerService.captureUnassignedTrainAsPosition` (bzw. Hilfsmethode) die Journey-Locations der neuesten Version → `List<TimetableRowData>` reverse-mappen → `TimetableArchive` (tableData-JSON) erzeugen + speichern, an die Position koppeln (wie der Vorwärtsfluss, aber ohne neuen PM-Train), + CAPACITY-`ResourceNeed` mit `linkedFahrplanId`. Bestehende Bausteine aus `TimetableArchiveService` wiederverwenden.
**Dateien:** `PathManagerService` (+ ggf. `TimetableArchiveService`-Hilfsmethode), Reverse-Mapping. **Risiko:** mittel (tableData-Format korrekt treffen). Test: erfassen → „Fahrplan öffnen" zeigt Tabelle.

## Slice H — Offene-Positionen-Übersicht (SOB S.21/28)
**Lösung:** Neue View `OffenePositionenView` (`/offene-positionen`, Nav-Eintrag), die „offene" Arbeit bündelt:
- Positionen mit ≥1 Bestellposition **nicht** im Status `BOOKED` (TTT) bzw. nicht `BESTELLT/BESTAETIGT` — d.h. noch zu bestellen/zu klären.
- **Unzugewiesene Fahrpläne** (`sourcePositionId IS NULL`) — Verknüpfung zur „Aus RailOpt"-Übernahme.
Jede Zeile verlinkt zum Auftrag/zur Position. Read-only Übersicht + Sprung-Links.
**Dateien:** neue View + Query-Methode(n) (`OrderPositionRepository`/`PurchasePositionRepository`), `MainLayout`-Nav, i18n. **Risiko:** mittel.

## Slice I — Massenverarbeitung: Bulk-Statuswechsel (SOB S.18/26)
**Lösung:** Im `OrderPositionPanel` Auswahl-Checkboxen pro Position (im Mutator-Modus) + eine Sammel-Aktionsleiste „n ausgewählt → Bearbeitungs-Status setzen". Service `OrderService.setPositionInternalStatusBulk(Set<UUID>, PositionStatus)` (eine Transaktion). Auswahl-State im Panel.
**Dateien:** `OrderPositionRow` (Checkbox), `OrderPositionPanel` (Auswahlleiste), `OrderService` (Bulk-Methode), i18n. **Risiko:** mittel (UI-State + Interaktion mit Kompakt/Filter).

## Slice J — Abweichungs-Erkennung (SOB S.26) — „Beides"
**Lösung:** Neuer `DeviationService` mit zwei Vergleichen für FAHRPLAN-Positionen mit `pmReferenceTrainId`:
1. **Auftrag ↔ RailOpt:** Positions-`fromLocation/toLocation/start/end` vs. Referenz-Train (`calendarStart/End`, erste/letzte Journey-Location) → Divergenzen.
2. **Version ↔ Original:** neueste `PmTrainVersion` vs. `INITIAL`-Version (geänderte Zeiten/Halte = Path-Modification/Alteration).
Ergebnis als ⚠-Abweichungs-Badge in `OrderPositionRow` + Detail (Liste der Abweichungen) im Aufklapp-Body bzw. Tooltip. Seed `V38`: eine zusätzliche abweichende `PmTrainVersion` (v2) auf einem bestehenden/Seed-Train, damit die Demo eine Version-Abweichung zeigt; für Auftrag↔RailOpt genügt eine bewusst abweichende Position.
**Dateien:** neuer `DeviationService`, `OrderPositionRow` (Badge), evtl. `V38`-Seed, i18n. **Risiko:** mittel-hoch (Datenmodell + Demo-Daten).

---

## Abschluss
- **E2E (Playwright als `dispatcher`)**: Jahresbestellung-Badge sichtbar; „Aus RailOpt" → Position → „Fahrplan öffnen" zeigt Tabelle; Offene-Positionen-View listet; Bulk-Statuswechsel auf Auswahl; ⚠-Abweichungs-Badge. Screenshots + DB-Gegencheck.
- **E2E-Demo-Daten zurücksetzen** (Mock-Reset, ADMIN — ggf. via `sebastian`/Admin oder PathManagerService.clearAllMockData bzw. gezieltes Löschen der Test-Artefakte).
- **Push** `feature/railopt-demo-prep`.

## Reihenfolge
F (Seed) → G (Anreichern) → H (Offene Positionen) → I (Bulk) → J (Abweichung) → E2E + Reset + Push.
