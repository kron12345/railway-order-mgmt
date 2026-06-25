# Bauplan: Zug-Identität/Ausprägungen/Versionen + Frist-Regeln/automatische Geschäfte

> Umsetzung der beiden Konzepte (`concept-train-identity-variants-versioning.md`,
> `concept-deadline-rules-auto-business.md`). Branch `feature/train-identity-variants` ab
> `feature/railopt-demo-prep`. Slice-weise: je Slice Compile/Spotless/ArchUnit, Commit, Codex-Review,
> am Ende E2E. Seed-getrieben, damit alles **optisch sichtbar** ist.

**User-Entscheide (2026-06-25):** Kalender⇒Kind / Zeit+Fahrzeug⇒Version · Kinder primär aus RailOpt,
manuell ergänzbar · Auto-Bestellung **beides** (Demo-Knopf + aktiver `@Scheduled`) · Konzept 1 zuerst
(Konzept 2 braucht die Verkehrstage-Ebene).

## Konzept 1 — Zug → Ausprägungen → Versionen

### K1-S1 · Datenmodell
- `OrderPosition`: `variantOf` als **Eltern-Link** aktivieren (Ausprägung → Zug); `variantType` als
  Enum `PositionVariantType` (`ZUG` | `AUSPRAEGUNG`; `null` = Bestand/flach). Convenience
  `children` (`@OneToMany mappedBy variantOf`).
- Neues Entity `OrderPositionVersion`: `orderPosition` (Kind), `versionNumber`, `source`
  (`VersionSource`: `INITIAL`/`MODIFICATION`/`ALTERATION`/`CANCELLATION`), `validFrom`/`validTo`
  (Gültigkeitsfenster des Overrides), `changeSummary` (kurzer Text), optional `payload` (JSON), Audit.
- Neue Tabelle `position_otn_history`: `order_position_id`, `otn`, `valid_from`, `valid_to`, `source`.
- Flyway `V39` (Spalten/Tabellen + `_audit`), `@Audited`, Repositories.
- **Verify:** Compile/Spotless/ArchUnit; V39 in Rollback-Tx gegen DB.

### K1-S2 · UI Zug → Ausprägungen
- `OrderPositionPanel`/`OrderPositionRow`: rendert den Zug als Container mit aufklappbaren
  **Ausprägungs-Kindern** (Verkehrstage · OTN · Route · Zeit · `PurchaseStatusRollup`).
- **Bestands-Migration:** flache FAHRPLAN-Position = Zug mit genau einem Kind (degeneriert) → altes
  Rendering bleibt. Migration/Adapter, kein Datenverlust.
- Seed `V40`: ein Zug mit zwei Ausprägungen (Mo–Fr 2×Flirt / Sa–So 1×Flirt).
- **Verify:** E2E-Screenshot zeigt Zug mit zwei Kindern.

### K1-S3 · Versionen + Änderungs-Feed
- Version-Timeline je Kind (Basis + Overrides mit Gültigkeitsfenster); effektive-Konfig-Auflösung pro
  Tag (Helper).
- `DeviationDetector` generalisieren → Versions-Vergleich + Quelle (Modification/Alteration);
  ⚠-Badge je Kind/Zug; **Änderungs-Feed** über den ganzen Zug; OTN-Historie-Chip am Header.
- Seed `V41`: Ferien-Traverso (KW 28–31, Modification) + eine Alteration (Ankunft +5 min).
- **Verify:** E2E zeigt Feed + Badge + OTN-Historie.

### K1-S4 · RailOpt-Spiegel + manuelles Anlegen
- Capture/Sync eines ReferenceTrains mit mehreren SubCalendars/Paths → Eltern + Kinder automatisch;
  **Disjunktheits-Check** der Verkehrstage.
- Manuelles „+ Ausprägung" im UI (mit gleichem Check).
- **Verify:** „Aus RailOpt" erzeugt Zug+Kinder; manuelles Anlegen blockt überlappende Tage.

## Konzept 2 — Frist-Regeln / automatische Geschäfte

### K2-S5 · FristRegel + Frist-Berechnung + Übersicht
- Entity `FristRegel`: Name, Mitglieds-Filter (Status/Typ), Anker (`ABSOLUT`/`FAHRT`/`TTR_PHASE`/
  `FAHRPLANJAHR_START`) + Offset, Warn-Schwelle, Trigger (Datum/Status), Aktion (Anzeigen/
  Auto-Bestellung). Admin-CRUD-UI (GUI-konfigurierbar).
- `FristService`: effektive Frist je Position/**Verkehrstag** rechnen, in Übersicht je Position auf die
  nächste offene Fahrt aggregieren.
- Fristen-Übersicht als Erweiterung der `OffenePositionenView` (überfällig / ≤N Tage / ok) +
  Frist-Chip je Position. Seed `V42`: zwei Beispiel-Regeln (Final-Offer-Jahr fix; „−2 T vor Fahrt").
- **Verify:** Übersicht gruppiert korrekt; Chips zeigen berechnete Fristen.

### K2-S6 · Automatische Geschäfte
- Regelgetriebene `Business`-Mitgliedschaft (**dynamisch**, nicht physisch verschoben); Markierung
  „⚙ automatisch" in der Geschäfte-Liste; Mitglieder + nächste/überfällige Frist.
- **Verify:** automatisches Geschäft listet die passenden Positionen.

### K2-S7 · Auto-Bestellung (Demo-Knopf + @Scheduled)
- `FristRegelEvaluator`: Knopf „Regeln auswerten" **und** aktiver `@Scheduled`-Job; Trigger datums-
  **und** statusbasiert (TTT `pmProcessState` = Final Offer → Event-Hook).
- Auto-Auslösung über `PurchaseOrderService` (TTT/R2P) mit **Audit jeder Auslösung**,
  **Idempotenz-Guard** (nicht doppelt), **Kill-Switch je Regel**.
- Seed/Status so, dass eine Final-Offer-Position vorführbar auto-bestellt wird.
- **Verify:** Knopf löst sichtbar eine Bestellung aus; @Scheduled feuert ebenfalls (Log/DB).

## Abschluss
- Vollständiger E2E-Durchlauf (dispatcher) über alle neuen Flächen; Screenshots + DB-Gegencheck.
- E2E-Demo-Daten zurücksetzen (Baseline-Dump/Restore wie bei F–J).
- Push. Danach: Pagination, dann Customer-/Geschäfts-Views.
