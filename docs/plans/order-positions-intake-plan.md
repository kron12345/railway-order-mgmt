# Plan: Auftragspositionen-Kompaktansicht, Status-Filter, SOB-Use-Cases & Intake

**Stand:** 2026-06-24 · **Branch:** `feature/railopt-demo-prep` · Status: FINAL (alle Fragen geklärt, bereit zur Umsetzung Slice für Slice)

## 0. Auslöser & Entscheidungen

User-Anforderung (Auftrag **4711** hat viele Auftragspositionen, „können leicht doppelt soviel sein"):
- **Kompaktansicht** zusätzlich, leicht umschaltbar „alle ↔ kompakt", alles ein-/ausklappbar, einzelne Positionen auf-/zuklappbar. Kompakt zeigt nur **Status der Bestellpositionen** + **von/nach/start/ende**.
- **Filter erweitern** → nach den verschiedenen **Status** filtern.
- **SOB-Spec-Lücken**: Jahresbestellung & Einzelbestellung prüfen.
- **„Nicht zugewiesen"**: Fahrplan im Fahrplanmanager, der noch nicht im Auftragsmanagement ist, als Auftragsposition erfassbar machen (Mock genügt).
- **R2P-Eingang**: eingehende Bestellung (jemand sendet Fahrplan + bestellt Personal/Fahrzeug) als Auftragsposition erfassbar (Mock genügt).

**Getroffene Entscheidungen (User, 2026-06-24):**
1. **Scope:** Alles als **ein großer Plan**, danach Umsetzung Slice für Slice.
2. **Kompakt-Status-Rollup:** **Beide kombiniert** — externer TTT-Status + Beschaffungs-Status als Mini-Badge-Cluster pro Position.
3. **Filter:** **Beide** — Positions-Filter im Auftrag *und* erweiterte Filter in der Übersicht.
4. **Jahres/Einzel:** **Automatisch aus TTR-Phase ableiten + nur anzeigen** (kein manuelles Setzen).

**Quellen:** `docs/private/SOB_concepts/Fachkonzept Auftragsmanagement 22587677.pdf` (31 S.), `…/Fachkonzept Umgang mit internen und externen Status …docx`; Code-Recon 2026-06-24.

---

## 1. Domänen-/Status-Abgleich (Ist vs. SOB)

**Gute Nachricht:** Das Status-Modell im Code deckt die SOB-Taxonomie bereits weitgehend ab.

| SOB-Fachkonzept | Code | Ebene |
|---|---|---|
| Interne Bearbeitungs-Status (In Bearbeitung, Freigegeben, Überarbeiten, Übermittelt, Beantragt, Abgeschlossen, Annulliert) | `PositionStatus` | Auftrag **und** Auftragsposition |
| Interne Prozess-Status für Aufträge (Auftrag→Planung→Produkt/Leistung→Produktion→Abrechnung) | `ProcessStatus` | Auftrag |
| Externe **TTT-Status** (Draft/Offered/Refused/Booked/Used/NotAvailable/Cancelled/Shadow) | `PurchasePosition.pmProcessState` (String, aus `PathProcessState`) | **Bestellposition** ✓ (Spec §5.8: TTT-Status nur auf Bestellpositions-Ebene) |
| RailOpt-Planungsstatus | `PmPlanningStatus` (UNPLANNED/PLANNED/ON_SHELF/ON_PHYSICAL_RESOURCE) | Fahrplan/PmReferenceTrain |
| Beschaffungs-Status | `PurchaseStatus` (OFFEN/BESTELLT/BESTÄTIGT/ABGELEHNT/STORNIERT) | Bestellposition |

**Objektmodell-Abgleich** (SOB S. 8-14 vs. Code):
- SOB: `Auftrag` (mit KTR **oder** administrativ) → `Auftragsposition` → `OTN` (+ Ausprägungen 2364.1/2364.2 je mit Verkehrstage-`Kalender`) → `Bestellposition` (pro OTN-Ausprägung × Kalender, alle PR-Daten). **Detailpositionen werden NICHT benötigt** (S. 13/25).
- Code: `Order` (hat `costCenter`) → `OrderPosition` (`FAHRPLAN`/`LEISTUNG`, trägt **von=`fromLocation`, nach=`toLocation`, start=`start`, ende=`end`**) → `ResourceNeed` (CAPACITY/VEHICLE/PERSONNEL) → `PurchasePosition` (Bestellposition).
- **Abweichung (nicht Teil dieses Plans, nur dokumentiert):** Die SOB-Zwischenebene „OTN-Ausprägung × Verkehrstage-Kalender" ist im Code nicht 1:1 abgebildet (Kalender hängt heute am Kauf/Validity, nicht als OTN-Subposition). Für Demo unkritisch.

**Begriffsklärung Anwendungsfälle (SOB):** AF1 = Jahresfahrplan (ATT New/Late PR) = **Jahresbestellung**; AF3 = Extrazug adhoc (ad-hoc PR) = **Einzelbestellung**; AF2 = Alteration/Baustellen (separat, nicht in diesem Plan).

---

## 2. Slice A — Kompaktansicht + Ein-/Ausklappen (Frontend)

**Ziel:** Auftrag mit vielen Positionen schnell überblickbar; pro Position einklappbar; globaler „Alle ↔ Kompakt"- und „Alle ein-/ausklappen"-Schalter.

**Ist:** `OrderPositionPanel` rendert je Position eine `OrderPositionRow` + (immer offen) das `ResourcePanel` darunter; kein Collapse, kein Toggle. Bestellpositions-Status nur **innen** im ResourcePanel sichtbar.

### Interaktions-/Layout-Design

Kopfzeile des `OrderPositionPanel` (rechts neben „+ Position"):
```
┌ Positionen (12) ───────────────────────────────────────────────┐
│  [ Alle ⌄ ]  [ Kompakt ▭ | Voll ▦ ]   [ Filter ⚑ 2 ]   [+ Position] │
└─────────────────────────────────────────────────────────────────┘
```
- **Segmented Control „Kompakt | Voll"**: schaltet den Default-Klappzustand aller Positionen um.
- **„Alle ⌄ / Alle ⌃"**: alle einklappen / ausklappen (ein Klick).

Jede Position wird zu einer **klappbaren Karte** (Vaadin `Details`, Summary = kompakte Kopfzeile, Content = bisheriges Voll-Rendering inkl. ResourcePanel). Im **Kompakt**-Modus sind alle `Details` zu (Content wird erst beim Aufklappen gerendert → löst zugleich das Performance-Thema bei doppelt so vielen Positionen).

**Kompakte Kopfzeile (Summary) — Informationshierarchie:**
```
▸ IR35 · OTN 2364   SG → RW   24.06. 08:12 → 09:47   │ 🟢3 booked · 🟡2 offered  ● 3 best. · ○ 2 offen
  └ Name/Linie+OTN    von→nach   start→ende           │   TTT-Rollup            Beschaffungs-Rollup
```
- **Signature-Element:** der **kombinierte Status-Rollup** rechts — zwei kompakte Zähler-Cluster (TTT + Beschaffung), aggregiert über alle Bestellpositionen der Position. Farb-Lumo-Badges (success/warning/contrast). Bei genau 1 Bestellposition: einzelnes Badge statt Zähler.
- Bestehende Lumo/Tailwind-Badge-Sprache wird **bewusst beibehalten** (internes Tool → Konsistenz schlägt Neuheit); die Gestaltungsentscheidung liegt in der *Verdichtung* (ein Cluster statt aufgeklappter Liste), nicht in einer neuen Optik.
- Aktionsbuttons (View/Edit/History/Delete/Kalender) wandern in die Summary-Zeile rechts bzw. bleiben im Content.

**Betroffene Dateien:** `OrderPositionPanel.java` (Header-Controls, Details-Wrapper, Klappzustand), `OrderPositionRow.java` (Summary-Variante + Rollup-Helper), neuer kleiner Helper `PurchaseStatusRollup` (zählt TTT- & Beschaffungs-Status über `position.getPurchasePositions()`), CSS in der Theme-Datei.

**Offene Detailfragen** (niedrig, mit sinnvollem Default umsetzbar):
- Klappzustand pro Auftrag (geht bei Navigation verloren) — Default: ja, kein Persistieren für den Mock.
- „Alle einklappen" schließt auch den Kalender-Toggle — Default: ja.

---

## 3. Slice B — Status-Filter (beide Ebenen)

**Ist:** Wiederverwendbares Filter-Framework (`ui/component/masterdetail/filter/`: `FilterPanel`, `SelectFilterField`, `DateRangeFilterField`, `TextFilterField`, `ToggleFilterField`) — heute nur in der **Auftragsübersicht** (`OrderOverviewView`) genutzt (ProcessStatus, Order.internalStatus, validFrom/To, Tags, „mir zugewiesen"). **Innerhalb** eines Auftrags kein Filter.

**B1 — Positions-Filter im Auftrag** (`OrderPositionPanel`):
- `FilterPanel<OrderPosition>` über den Positions-Karten.
- Felder (Multi-Select, „ODER" je Feld, „UND" über Felder):
  - **TTT-Status** der Bestellpositionen (Booked/Offered/…) — Prädikat: „Position hat ≥1 Bestellposition mit Status X" (konfigurierbar: „alle" vs „mind. eine").
  - **Beschaffungs-Status** (OFFEN/BESTELLT/…) analog.
  - **Bearbeitungs-Status** der Position (`PositionStatus`).
- Client-seitiges Prädikat auf bereits geladenen Positionen (keine Query-Änderung).
- Ggf. `MultiSelectFilterField` neu (heute nur Single-`SelectFilterField`) — kleiner Zusatz im Framework.

**B2 — Übersichts-Filter erweitern** (`OrderOverviewView`):
- Neue Filter: **TTR-Phase/Auftragstyp** (Jahresfahrplan/Extrazug, aus Slice C), **„Bestellung unvollständig"** (nicht alle Bestellpositionen `booked` — entspricht SOB §5.7 Produktionsübergabe-Check), optional TTT-Status-Aggregat.

**Betroffene Dateien:** `OrderPositionPanel.java`, evtl. neues `MultiSelectFilterField.java`, `OrderOverviewView.java`, i18n-Keys.

---

## 4. Slice C — Jahres-/Einzelbestellung (ableiten + anzeigen)

**Entscheidung:** automatisch aus TTR-Phase, nur Anzeige.

**Ist:** `TtrPhaseResolver` löst Phase aus Datum vs. Fahrplanwechsel-Meilensteinen: ANNUAL_NEW (X-11…X-8.5), ANNUAL_LATE (X-8.5…X-2), AD_HOC (X-2…). `PositionType` FAHRPLAN/LEISTUNG. **Kein** Auftragstyp.

**Umsetzung:**
- Abgeleiteter **Auftragstyp** (kein DB-Feld nötig): Helper `OrderTypeResolver` mappt TTR-Phase → `JAHRESFAHRPLAN` (ANNUAL_NEW/LATE) bzw. `EXTRAZUG` (AD_HOC) anhand `order.validFrom`/Positions-`start`.
- **Anzeige** als Badge in Auftragsliste (`OrderCard`) + Auftrags-Header (`OrderDetailView`) + im Kompakt-Filter (Slice B2). Farbe: Jahresfahrplan = neutral/contrast, Extrazug = warning.
- **SOB-Fluss-Abgleich dokumentieren** (AF1 vs AF3) im selben Doc; tatsächliche Abläufe (Capacity-Supply-Import, Massenverarbeitung, Final-Offer-Timer) sind **größere Features** → als „spätere Kandidaten" gelistet, nicht in diesem Slice.

**Spätere SOB-Kandidaten (nicht jetzt, nur Backlog):** Massenverarbeitung von OTN-Bändern; „Funktion, die offene Positionen ausweist" (→ Slice D deckt einen Teil); Final-Offer-Annahme-Timer mit Warnung; Abweichungs-Erkennung Jahresfahrplan↔Capacity-Supply.

**Betroffene Dateien:** neuer `OrderTypeResolver.java` (domain), `OrderCard.java`, `OrderDetailView.java`, i18n.

---

## 5. Slice D — „Nicht zugewiesen": Fahrplan aus Fahrplanmanager als Position erfassen (Mock)

**SOB-Basis:** *„Funktion, die offene Positionen ausweist"* (S. 21/28); Capacity-Supply-Import (nur SOB-relevante OTN auswählen → Aufträgen zuordnen, S. 18); vom Planer manuell angelegte Fahrten (Leermaterialzüge) einem Auftrag zuweisen.

**Ist:** Datenfluss ist Einbahn (Order → PathManager). `PmReferenceTrain.sourcePositionId` ist nullable → **null = nicht zugewiesen**. Es fehlen: Query für unzugewiesene, Reverse-Mapping, UI.

**Mock-Umsetzung:**
1. **Seed:** 2-3 `PmReferenceTrain` mit `sourcePositionId = null` (per Flyway-Seed oder Dev-Seeder) als „im Fahrplanmanager vorhanden, aber nicht im Auftragsmanagement".
2. **Query:** `PmReferenceTrainRepository.findBySourcePositionIdIsNull()`.
3. **Service:** `captureUnassignedTrainAsPosition(UUID trainId, UUID orderId)` — lädt Train + neueste Version + `PmJourneyLocation`s, reverse-mappt zu `TimetableRowData`, ruft bestehendes `TimetableArchiveService.saveTimetablePosition()` (erzeugt Archive + CAPACITY-ResourceNeed + OrderPosition), setzt `sourcePositionId = position.id`.
4. **UI:** Aktion „Nicht zugewiesene Fahrpläne" (Dialog oder Sektion) — Liste (TRID, OTN, Status, Jahr); User wählt Fahrplan + Ziel-Auftrag → Vorbelegung von/nach (erste/letzte JourneyLocation), start/ende (calendarStart/End), OTN → „Erfassen".

**Betroffene Dateien:** `PmReferenceTrainRepository.java`, `PathManagerService.java` (oder neuer `UnassignedTrainCaptureService`), neues UI-Dialog, ein Dev-Seed.

**Offene Detailfragen (Default vorgeschlagen):** von/nach = erste/letzte JourneyLocation (Default ja); Validität aus calendarStart/End (Default ja); Reverse-Mapping nur Kernfelder (sequence/name/Zeiten) für den Mock (Default ja).

---

## 6. Slice E — R2P-Eingang als Position erfassen (Mock) ⚠️ braucht Freigabe

**Wichtig:** R2P ist im SOB-Fachkonzept **ausdrücklich Out-of-Scope** (S. 6) und **nirgends spezifiziert**. Es gibt keine fachliche Vorlage → wir entwerfen den Mock frei. Strukturell nächster Anker: das **Laufkarte-Formular** (S. 17) mit „gewünschten Ressourcen" (Lokpersonal, Kundenbegleiter, Fahrzeug, Sicherheitsdienst …).

**Ist:** Nur `triggerR2pOrder()` (Mock OFFEN→BESTELLT für bestehende Bestellposition) + R²P-Badge in `ResourcePanel`. Kein **Eingang** (Intake).

**Bestätigter Mock (User 2026-06-24):**
1. **DTO** `R2pOrderDto`: { otn, von, nach, start, ende, angefragte Ressourcen[] (Typ PERSONNEL/VEHICLE, **Katalog-Referenz ODER Freitext**, Menge) }.
2. **Verknüpfung:** Erst nach passender **FAHRPLAN-Position per OTN** (sonst von/nach) suchen → Ressourcen **dort anhängen**; nur wenn nichts passt, **neue Position** anlegen.
3. **Annahme-Workflow: Inbox/Entwurf** — Eingang landet in einer „R2P-Eingang"-Liste (eigener Eingangs-Status, z.B. `EINGEGANGEN`); Bearbeiter sichtet und **übernimmt bewusst**. Erst bei Übernahme werden Position/ResourceNeeds/Bestellpositionen (`OFFEN`) erzeugt bzw. angehängt.
4. **Ressourcen:** **Katalog-Referenz wenn vorhanden, sonst Freitext-Fallback** (`PERSONNEL_QUAL`/`VEHICLE_TYPE`).
5. **Auslöser (Demo): UI-Knopf „R2P-Eingang simulieren"** in der R2P-Inbox erzeugt einen Beispiel-Eingang (kein externes Tool nötig). Optional zusätzlich REST `POST /api/v1/orders/r2p`.
6. **Herkunft:** neuer `ResourceOrigin.R2P` + R2P-Badge an Position/Ressource.

**Betroffene Dateien:** neues `R2pInboxEntry`-Modell + Repo (Inbox/Entwurf), `R2pIntakeService` (Matching per OTN + Übernahme), `R2pOrderDto`, neue „R2P-Eingang"-View mit „simulieren"-Knopf, `ResourceOrigin` (+R2P), optional `R2pOrderController`.

---

## 7. Umsetzungsreihenfolge (Slices)

| # | Slice | Abhängigkeit | Größe |
|---|---|---|---|
| A | Kompaktansicht + Collapse + Status-Rollup | – | M |
| B | Status-Filter (Position + Übersicht) | A (Rollup-Helper) | M |
| C | Jahres-/Einzel-Badge (TTR-Ableitung) | – | S |
| D | „Nicht zugewiesen" Fahrplan-Intake (Mock) | – | M |
| E | R2P-Eingang (Mock) | **R2P-Fragen geklärt** | M |

Jeder Slice: kompilieren + Spotless + ArchUnit/Tests + Boot/Smoke + Commit (+ Codex-Review), wie gehabt.

---

## 8. Offene Fragen

**R2P (Slice E) — GEKLÄRT (User 2026-06-24):** Verknüpfung = an bestehende FAHRPLAN-Position per OTN anhängen, sonst neu · Annahme = Inbox/Entwurf, dann übernehmen · Ressourcen = Katalog + Freitext-Fallback · Auslöser = UI-Knopf „R2P-Eingang simulieren". Siehe Slice E.

**Klein (mit Defaults umsetzbar, nur zur Info):** Klappzustand nicht persistieren (A); Positions-Filter „mind. eine Bestellposition matcht" als Default-Semantik (B); Capture-Defaults für von/nach/Validität (D).
