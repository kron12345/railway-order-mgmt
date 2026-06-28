# Konzept: Frist-Regeln & automatische Geschäfte (Final-Offer / Fälligkeiten)

> Status: **VOLLSTÄNDIG UMGESETZT** (2026-06-27, Branch `feature/overview-filters-and-deadlines`).
> User-Entscheid: 1b/2a/3b + Member-Filter; 4a (Mock) später auf echten Versand umgestellt. Gebaut:
> automatisches Geschäft = echtes `Business` (`AutoBusinessService`, V48), Frist je Verkehrstag
> (`FristService.perDay`), erweiterte Member-Filter, Admin-CRUD (`FristRegelAdminView`), Frist-Chip an
> der Position; **Feinschliff (2. Runde):** Anker `TTR_PHASE` (`TtrPhaseResolver.orderingDeadline`),
> Event-Listener auf den TTT-Status-Sync (`TttStatusChangedEvent`), vollständiger Audit-Trail
> (`AutoOrderLog` + V49), echte Auto-Bestellung über `PurchaseOrderService.triggerOrderForPosition`
> (System-Sicherheitskontext für Schedule/Event). **Offen:** Member-Filter „nicht verrechnet" (kein
> Abrechnungsmodell vorhanden). (Ersetzt die Idee eines separaten „Final-Offer-Timers".)

## Ziel

Fälligkeiten (Final-Offer-Annahme, Verrechnung, Fahrzeugzuweisung, …) **ohne eigenes Timer-Konstrukt**
abbilden — über bestehende **Geschäfte**, die regelbasiert automatisch befüllt werden und ein
automatisches Fälligkeitsdatum tragen. Beispiele:
- „Final-Offer Jahresfahrplan 2027" sammelt automatisch **alle noch nicht bestellten** Bestellpositionen,
  Frist = fixer Fahrplanjahr-Stichtag.
- AdHoc-Bestellungen mit **TTR-Fristigkeiten** (rollend).
- „Verrechnung spätestens **10 Tage nach Fahrt**", „Fahrzeugzuweisung spätestens **2 Tage vor Fahrt**".

## Kernidee: Frist-Regel = Filter + Anker + Aktion

Eine **Frist-Regel** ist konfigurierbar (Admin-UI) und hat drei Teile:

| Teil | Bedeutung | Beispiele |
|---|---|---|
| **Mitglieds-Filter** | welche Positionen/Bestellpositionen Mitglied sind | `purchaseStatus = OFFEN`; kein Fahrzeug zugewiesen; nicht verrechnet; Auftragstyp JAHRES/ADHOC |
| **Anker + Offset** | wie die Frist je Mitglied berechnet wird | siehe unten |
| **Aktion + Trigger** | was bei Fälligkeit/Status passiert | siehe unten |

### Anker (löst das Problem fixe vs. rollende Fenster)

Statt eines einzigen Geschäft-Datums wird die **effektive Frist je Mitglied berechnet**:

- **ABSOLUT** — fixes Datum (Fahrplanjahr-Stichtag). Gleiche Frist für alle Mitglieder → der einfache
  Fall „ein Datum fürs ganze Geschäft".
- **RELATIV** — `Anker(Mitglied) + Offset`:
  - Anker `FAHRT` → ±N Tage relativ zum Fahrt-Datum (rollend).
  - Anker `TTR_PHASE` → Stichtag aus `TtrPhaseResolver` (ADR-013), für AdHoc.
  - Anker `FAHRPLANJAHR_START` → ±N Tage relativ zum Jahresstart.

**Wichtige Konsequenz (rollende Fenster):** „2 Tage vor Fahrt" / „10 Tage nach Fahrt" ist streng eine
Frist **pro Fahrt-/Verkehrstag**, nicht pro Position — das hängt direkt an der Ausprägungs-/
Verkehrstage-Ebene aus `concept-train-identity-variants-versioning.md`. Designentscheid für die
Bau-Phase: Frist je **Verkehrstag** rechnen, in der Übersicht je Position auf die **nächste offene
Fahrt** aggregieren (Detail je Tag aufklappbar).

### Aktion + Trigger (User-Entscheid: vollautomatisch)

- **Trigger** kann **datums-** und/oder **statusbasiert** sein:
  - datumsbasiert: berechnete Frist erreicht (bzw. Warn-Schwelle N Tage vorher).
  - statusbasiert: TTT-Status wechselt (z. B. `pmProcessState` = **Final Offer**).
- **Aktion = Auto-Bestellung vollautomatisch:** bei Trigger wird die Bestellung **direkt** ausgelöst
  (kein Bestätigungsschritt) über den bestehenden `PurchaseOrderService` (TTT-/R2P-Auslösung). Überfällig-/
  Fällig-Anzeige läuft immer mit.
- ⚠ **Dokumentierte Design-Risiken** (bewusst akzeptiert für Demo/Mock): vollautomatisches Bestellen
  ohne Bestätigung braucht in Produktion einen Audit-Trail jeder Auto-Auslösung, Idempotenz (nicht
  doppelt bestellen) und einen Not-Aus/Kill-Switch je Regel. Diese Punkte werden im Bau mitgebaut bzw.
  als Folge-Backlog markiert.

## „Automatisches Geschäft"

- Ein **Geschäft** (bestehende `Business`-Entity) wird **regelgetrieben**: Mitgliedschaft = Filter der
  Frist-Regel (**dynamisch**, Positionen werden nicht physisch verschoben), Fälligkeit = berechnete
  Frist.
- Nutzt die bereits gebauten **n:m-Verknüpfungen Business ↔ OrderPosition/PurchasePosition** und die
  `AssigneeComboBox`/Status-Bausteine.
- Ein periodischer Evaluator (`@Scheduled`) frischt Mitgliedschaft + Fristen auf; ein Event-Hook auf den
  TTT-Status-Sync feuert statusbasierte Trigger sofort.

## Mechanik

```
@Scheduled / Event (TTT-Status-Sync)
        │
        ▼
FristRegelEvaluator
   für jede Regel:
     members   = Filter über offene Positionen/Bestellpositionen
     je member: frist = Anker(member) + Offset        (pro Verkehrstag)
     überfällig/fällig flaggen
     wenn Trigger erfüllt (Frist erreicht ODER TTT-Status):
         PurchaseOrderService.triggerTtt/R2p(member)   ← vollautomatisch
         Auto-Auslösung protokollieren (Audit, idempotent)
```

## UI-Skizze

- **Admin — Frist-Regeln (CRUD):** Name · Mitglieds-Filter · Anker (ABSOLUT/FAHRT/TTR_PHASE/JAHR) +
  Offset · Trigger (Datum/Status) · Aktion (Anzeigen / Auto-Bestellung) · Warn-Schwelle.
- **Geschäfte-Liste:** automatische Geschäfte als solche markiert („⚙ automatisch"), mit Mitglieder-
  Zahl + nächster/überfälliger Frist.
- **Fristen-Übersicht:** Erweiterung der `OffenePositionenView` → Gruppierung **überfällig / fällig in
  ≤N Tagen / ok** (nutzt die `dashboardStats.criticalDeadlines`-Logik).
- **Pro Position:** „Frist"-Chip mit nächstem Fälligkeitsdatum + auslösender Regel.

## Mapping auf den heutigen Code (wiederverwendbar)

| Baustein | Vorhanden |
|---|---|
| Geschäft + n:m-Links | `Business`, `business_order_positions`/`business_purchase_positions` |
| Auto-Bestellung | `PurchaseOrderService` (TTT-/R2P-Auslösung, `triggerR2pOrder`, Alteration-Flow) |
| TTR-Anker | `TtrPhaseResolver` (ADR-013) |
| Fällig-Logik/Übersicht | `OrderService.dashboardStats().criticalDeadlines`, `OffenePositionenView` |
| Status-Trigger | TTT-Sync (`PurchasePosition.pmProcessState`/`pmTtrPhase`, `pmLastSynced`) |

## Neu zu bauen

- `FristRegel`-Entity + Admin-CRUD-UI (konfigurierbar, GUI — kein Hardcoding).
- `FristRegelEvaluator` (Service, `@Scheduled` + Event-Listener auf TTT-Status-Sync).
- Frist-Berechnung je Verkehrstag (hängt an der Verkehrstage-Ebene des Schwester-Konzepts).
- Auto-Auslösungs-Audit + Idempotenz-Guard + Kill-Switch je Regel.

## Abhängigkeit

Rollende, fahrt-relative Fristen brauchen die **Verkehrstage-Ebene** aus
`concept-train-identity-variants-versioning.md`. Fixe (Fahrplanjahr-)Fristen gehen auch ohne. Reihenfolge
im Bau daher: erst Verkehrstage-Modell, dann rollende Fristen; absolute Fristen sind vorab machbar.
